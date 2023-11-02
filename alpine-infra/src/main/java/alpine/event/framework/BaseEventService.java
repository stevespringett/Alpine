/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.event.framework;

import alpine.common.logging.Logger;
import alpine.common.metrics.Metrics;
import alpine.model.EventServiceLog;
import alpine.persistence.AlpineQueryManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static alpine.common.util.ExecutorUtil.getExecutorStats;

/**
 * A publish/subscribe (pub/sub) event service that provides the ability to publish events and
 * asynchronously inform all subscribers to subscribed events.
 *
 * Defaults to a single thread event system when extending this class. This can be changed by
 * specifying an alternative executor service.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public abstract class BaseEventService implements IEventService {

    private Logger logger = Logger.getLogger(BaseEventService.class);
    private final Map<Class<? extends Event>, ArrayList<Class<? extends Subscriber>>> subscriptionMap = new ConcurrentHashMap<>();
    private final Map<UUID, ArrayList<UUID>>chainTracker = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newFixedThreadPool(1, new BasicThreadFactory.Builder()
            .namingPattern("Alpine-BaseEventService-%d")
            .uncaughtExceptionHandler(new LoggableUncaughtExceptionHandler())
            .build()
    );
    private final ExecutorService dynamicExecutor = Executors.newWorkStealingPool();

    /**
     * @param executor an ExecutorService instance
     * @since 1.0.0
     */
    protected void setExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * @param logger the logger instance to use for the executed event
     * @since 1.0.0
     */
    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     * @since 1.0.0
     */
    public void publish(Event event) {
        logger.debug("Dispatching event: " + event.getClass().toString());
        final ArrayList<Class<? extends Subscriber>> subscriberClasses = subscriptionMap.get(event.getClass());
        if (subscriberClasses == null) {
            logger.debug("No subscribers to inform from event: " + event.getClass().getName());
            return;
        }
        for (Class<? extends Subscriber> clazz: subscriberClasses) {
            logger.debug("Alerting subscriber " + clazz.getName());

            if (event instanceof ChainableEvent) {
                if (! addTrackedEvent((ChainableEvent)event)) {
                    return;
                }
            }

            // Check to see if the Event is Unblocked. If so, use a separate executor pool from normal events
            final ExecutorService executorService = event instanceof UnblockedEvent  ? dynamicExecutor : executor;

            executorService.execute(() -> {
                try (AlpineQueryManager qm = new AlpineQueryManager()) {
                    final EventServiceLog eventServiceLog = qm.createEventServiceLog(clazz);
                    final Subscriber subscriber = clazz.getDeclaredConstructor().newInstance();
                    final Timer.Sample timerSample = Timer.start();
                    try {
                        subscriber.inform(event);
                    } finally {
                        timerSample.stop(Timer.builder("alpine_event_processing")
                                .tag("event", event.getClass().getSimpleName())
                                .tag("subscriber", clazz.getSimpleName())
                                .register(Metrics.getRegistry()));
                    }
                    qm.updateEventServiceLog(eventServiceLog);
                    if (event instanceof ChainableEvent) {
                        ChainableEvent chainableEvent = (ChainableEvent)event;
                        logger.debug("Calling onSuccess");
                        for (ChainLink chainLink: chainableEvent.onSuccess()) {
                            if (chainLink.getSuccessEventService() != null) {
                                Method method = chainLink.getSuccessEventService().getMethod("getInstance");
                                IEventService es = (IEventService) method.invoke(chainLink.getSuccessEventService(), new Object[0]);
                                es.publish(chainLink.getSuccessEvent());
                            } else {
                                Event.dispatch(chainLink.getSuccessEvent());
                            }
                        }
                    }
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | SecurityException e) {
                    logger.error("An error occurred while informing subscriber: " + e);
                    if (event instanceof ChainableEvent) {
                        ChainableEvent chainableEvent = (ChainableEvent)event;
                        logger.debug("Calling onFailure");
                        for (ChainLink chainLink: chainableEvent.onFailure()) {
                            if (chainLink.getFailureEventService() != null) {
                                try {
                                    Method method = chainLink.getFailureEventService().getMethod("getInstance");
                                    IEventService es = (IEventService) method.invoke(chainLink.getFailureEventService(), new Object[0]);
                                    es.publish(chainLink.getFailureEvent());
                                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                                    logger.error("Exception while calling onFailure callback", ex);
                                }
                            } else {
                                Event.dispatch(chainLink.getFailureEvent());
                            }
                        }
                    }
                } finally {
                    if (event instanceof ChainableEvent) {
                        removeTrackedEvent((ChainableEvent)event);
                    }
                }
            });
        }
        recordPublishedMetric(event);
    }

    /**
     * {@inheritDoc}
     * @since 1.4.0
     */
    public synchronized boolean isEventBeingProcessed(ChainableEvent event) {
        return isEventBeingProcessed(event.getChainIdentifier());
    }

    /**
     * {@inheritDoc}
     * @since 1.4.0
     */
    public synchronized boolean isEventBeingProcessed(UUID chainIdentifier) {
        ArrayList<UUID> eventIdentifiers = chainTracker.get(chainIdentifier);
        return eventIdentifiers != null && eventIdentifiers.size() != 0;
    }

    private synchronized boolean addTrackedEvent(ChainableEvent event) {
            ArrayList<UUID> eventIdentifiers = chainTracker.get(event.getChainIdentifier());
            if (eventIdentifiers == null) {
                eventIdentifiers = new ArrayList<>();
            }
            if (event instanceof SingletonCapableEvent) {
                final SingletonCapableEvent sEvent = (SingletonCapableEvent)event;
                // Check is this is a singleton event where only a
                // single occurrence should be running at a given time
                if (sEvent.isSingleton()) {
                    if (! eventIdentifiers.isEmpty()) {
                        logger.info("An singleton event (" + sEvent.getClass().getSimpleName() + ") was received but another singleton event of the same type is already in progress. Skipping.");
                        return false;
                    }
                }
            }
            eventIdentifiers.add(event.getEventIdentifier());
            chainTracker.put(event.getChainIdentifier(), eventIdentifiers);
            return true;
    }

    private synchronized void removeTrackedEvent(ChainableEvent event) {
        ArrayList<UUID> eventIdentifiers = chainTracker.get(event.getChainIdentifier());
        if (eventIdentifiers == null) {
            return;
        }
        eventIdentifiers.remove(event.getEventIdentifier());
        if (eventIdentifiers.isEmpty()) {
            chainTracker.remove(event.getChainIdentifier());
        }
    }

    private void recordPublishedMetric(final Event event) {
        Counter.builder("alpine_events_published_total")
                .description("Total number of published events")
                .tags("event", event.getClass().getName(), "publisher", this.getClass().getName())
                .register(Metrics.getRegistry())
                .increment();
    }

    /**
     * {@inheritDoc}
     * @since 1.0.0
     */
    public void subscribe(Class<? extends Event> eventType, Class<? extends Subscriber> subscriberType) {
        if (!subscriptionMap.containsKey(eventType)) {
            subscriptionMap.put(eventType, new ArrayList<>());
        }
        final ArrayList<Class<? extends Subscriber>> subscribers = subscriptionMap.get(eventType);
        if (!subscribers.contains(subscriberType)) {
            subscribers.add(subscriberType);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.0.0
     */
    public void unsubscribe(Class<? extends Subscriber> subscriberType) {
        for (ArrayList<Class<? extends Subscriber>> list : subscriptionMap.values()) {
            list.remove(subscriberType);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    public boolean hasSubscriptions(Event event) {
        final ArrayList<Class<? extends Subscriber>> subscriberClasses = subscriptionMap.get(event.getClass());
        return subscriberClasses != null;
    }

    /**
     * {@inheritDoc}
     * @since 1.0.0
     */
    public void shutdown() {
        logger.info("Shutting down EventService");
        executor.shutdown();
        dynamicExecutor.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shutdown(final Duration timeout) {
        shutdown();

        final Instant waitTimeout = Instant.now().plus(timeout);
        Instant statsLastLoggedAt = null;
        while (!executor.isTerminated() || !dynamicExecutor.isTerminated()) {
            if (waitTimeout.isBefore(Instant.now())) {
                logger.warn("Timeout exceeded while waiting for executors to finish: executor=%s, dynamicExecutor=%s"
                        .formatted(getExecutorStats(executor), getExecutorStats(dynamicExecutor)));
                return false;
            }

            final Instant now = Instant.now();
            if (statsLastLoggedAt == null || now.minus(5, ChronoUnit.SECONDS).isAfter(statsLastLoggedAt)) {
                logger.info("Waiting for executors to terminate: executor=%s, dynamicExecutor=%s"
                        .formatted(getExecutorStats(executor), getExecutorStats(dynamicExecutor)));
                statsLastLoggedAt = now;
            }
        }

        logger.info("Executors terminated successfully");
        return true;
    }

}

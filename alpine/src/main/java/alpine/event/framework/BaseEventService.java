/*
 * This file is part of Alpine.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package alpine.event.framework;

import alpine.logging.Logger;
import alpine.model.EventServiceLog;
import alpine.persistence.AlpineQueryManager;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private Map<Class<? extends Event>, ArrayList<Class<? extends Subscriber>>> subscriptionMap = new ConcurrentHashMap<>();
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

            // Check to see if the Event is Unblocked. If so, use a separate executor pool from normal events
            final ExecutorService executorService = event instanceof UnblockedEvent  ? dynamicExecutor : executor;

            executorService.execute(() -> {
                try (AlpineQueryManager qm = new AlpineQueryManager()) {
                    final EventServiceLog eventServiceLog = qm.createEventServiceLog(clazz);
                    final Subscriber subscriber = clazz.getDeclaredConstructor().newInstance();
                    subscriber.inform(event);
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
                }
            });
        }
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

}

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
package alpine.notification;

import alpine.common.logging.Logger;
import alpine.common.metrics.Metrics;
import alpine.event.framework.LoggableUncaughtExceptionHandler;
import io.micrometer.core.instrument.Counter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static alpine.common.util.ExecutorUtil.getExecutorStats;

/**
 * The NotificationService provides a way to dispatch (publish) notifications to zero or more subscribers.
 * Architecturally, the NotificationService is similar to {@link alpine.event.framework.IEventService} but
 * the objectives and logic of notification processing vary enough to warrant implementations specific to
 * notifications.
 *
 * @author Steve Springett
 * @since 1.3.0
 */
public final class NotificationService implements INotificationService {

    private static final NotificationService INSTANCE = new NotificationService();
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class);
    private static final Map<Class<? extends Notification>, ArrayList<Subscription>> SUBSCRIPTION_MAP = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR_SERVICE;
    private static final String EXECUTOR_SERVICE_NAME = "Alpine-NotificationService";

    static {
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern(EXECUTOR_SERVICE_NAME + "-%d")
                .uncaughtExceptionHandler(new LoggableUncaughtExceptionHandler())
                .build();
        EXECUTOR_SERVICE = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), factory);
        Metrics.registerExecutorService(EXECUTOR_SERVICE, EXECUTOR_SERVICE_NAME);
    }

    /**
     * Private constructor
     */
    private NotificationService() {
    }

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public void publish(final Notification notification) {
        LOGGER.debug("Dispatching notification: " + notification.getClass().toString());
        final ArrayList<Subscription> subscriptions = SUBSCRIPTION_MAP.get(notification.getClass());
        if (subscriptions == null) {
            LOGGER.debug("No subscribers to inform from notification: " + notification.getClass().getName());
            return;
        }
        for (final Subscription subscription : subscriptions) {
            if (subscription.getScope() != null && subscription.getGroup() != null && subscription.getLevel() != null) { // subscription was the most specific
                if (subscription.getScope().equals(notification.getScope()) && subscription.getGroup().equals(notification.getGroup()) && subscription.getLevel() == notification.getLevel()) {
                    alertSubscriber(notification, subscription.getSubscriber());
                }
            } else if (subscription.getGroup() != null && subscription.getLevel() != null) { // subscription was very specific
                if (subscription.getGroup().equals(notification.getGroup()) && subscription.getLevel() == notification.getLevel()) {
                    alertSubscriber(notification, subscription.getSubscriber());
                }
            } else if (subscription.getGroup() != null) { // subscription was somewhat specific
                if (subscription.getGroup().equals(notification.getGroup())) {
                    alertSubscriber(notification, subscription.getSubscriber());
                }
            } else if (subscription.getLevel() != null) { // subscription was somewhat specific
                if (subscription.getLevel() == notification.getLevel()) {
                    alertSubscriber(notification, subscription.getSubscriber());
                }
            } else { // subscription was not specific
                alertSubscriber(notification, subscription.getSubscriber());
            }
        }
        recordPublishedMetric(notification);
    }

    private void alertSubscriber(final Notification notification, final Class<? extends Subscriber> subscriberClass) {
        LOGGER.debug("Alerting subscriber " + subscriberClass.getName());
        EXECUTOR_SERVICE.execute(() -> {
            try {
                subscriberClass.getDeclaredConstructor().newInstance().inform(notification);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | SecurityException e) {
                LOGGER.error("An error occurred while informing subscriber: " + e);
            }
        });
    }

    private void recordPublishedMetric(final Notification notification) {
        Counter.builder("alpine_notifications_published_total")
                .description("Total number of published notifications")
                .tags(
                        "group", notification.getGroup(),
                        "level", notification.getLevel().name(),
                        "scope", notification.getScope()
                )
                .register(Metrics.getRegistry())
                .increment();
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public void subscribe(final Class<? extends Notification> notificationClass, final Subscription subscription) {
        if (!SUBSCRIPTION_MAP.containsKey(notificationClass)) {
            SUBSCRIPTION_MAP.put(notificationClass, new ArrayList<>());
        }
        final ArrayList<Subscription> subscriptions = SUBSCRIPTION_MAP.get(notificationClass);
        if (!subscriptions.contains(subscription)) {
            subscriptions.add(subscription);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public void subscribe(final Subscription subscription) {
        subscribe(Notification.class, subscription);
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public void unsubscribe(final Subscription subscription) {
        for (final ArrayList<Subscription> list : SUBSCRIPTION_MAP.values()) {
            list.remove(subscription);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public boolean hasSubscriptions(final Notification notification) {
        final ArrayList<Subscription> subscriptions = SUBSCRIPTION_MAP.get(notification.getClass());
        return subscriptions != null;
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public void shutdown() {
        LOGGER.info("Shutting down NotificationService");
        EXECUTOR_SERVICE.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    public boolean shutdown(final Duration timeout) {
        shutdown();

        final Instant waitTimeout = Instant.now().plus(timeout);
        Instant lastStatsLog = null;
        while (!EXECUTOR_SERVICE.isTerminated()) {
            if (waitTimeout.isBefore(Instant.now())) {
                LOGGER.warn("Timeout exceeded while waiting for executor to finish: %s"
                        .formatted(getExecutorStats(EXECUTOR_SERVICE)));
                return false;
            }

            final Instant now = Instant.now();
            if (lastStatsLog == null || now.minus(3, ChronoUnit.SECONDS).isAfter(lastStatsLog)) {
                LOGGER.info("Waiting for executor to terminate: %s".formatted(getExecutorStats(EXECUTOR_SERVICE)));
                lastStatsLog = now;
            }
        }

        LOGGER.info("Executor terminated gracefully");
        return true;
    }

}

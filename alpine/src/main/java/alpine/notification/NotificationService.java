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
package alpine.notification;

import alpine.event.framework.LoggableUncaughtExceptionHandler;
import alpine.logging.Logger;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4,
            new BasicThreadFactory.Builder()
                    .namingPattern("Alpine-NotificationService-%d")
                    .uncaughtExceptionHandler(new LoggableUncaughtExceptionHandler())
                    .build()
    );

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
    public void publish(Notification notification) {
        LOGGER.debug("Dispatching notification: " + notification.getClass().toString());
        final ArrayList<Subscription> subscriptions = SUBSCRIPTION_MAP.get(notification.getClass());
        if (subscriptions == null) {
            LOGGER.debug("No subscribers to inform from notification: " + notification.getClass().getName());
            return;
        }
        for (Subscription subscription: subscriptions) {
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
    }

    private void alertSubscriber(Notification notification, Class<? extends Subscriber> subscriberClass) {
        LOGGER.debug("Alerting subscriber " + subscriberClass.getName());
        EXECUTOR_SERVICE.execute(() -> {
            try {
                subscriberClass.getDeclaredConstructor().newInstance().inform(notification);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | SecurityException e) {
                LOGGER.error("An error occurred while informing subscriber: " + e);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public void subscribe(Class<? extends Notification> notificationClass, Subscription subscription) {
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
    public void subscribe(Subscription subscription) {
        subscribe(Notification.class, subscription);
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public void unsubscribe(Subscription subscription) {
        for (ArrayList<Subscription> list : SUBSCRIPTION_MAP.values()) {
            list.remove(subscription);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.3.0
     */
    public boolean hasSubscriptions(Notification notification) {
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

}

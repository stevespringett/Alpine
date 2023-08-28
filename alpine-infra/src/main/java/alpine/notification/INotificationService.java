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

import java.time.Duration;

/**
 * Defines a NotificationService. All notification services must be singletons and implement a static
 * getInstance() method.
 *
 * @author Steve Springett
 * @since 1.3.0
 */
public interface INotificationService {

    /**
     * Publishes Notification. Published notifications will get dispatched to all subscribers in the order in
     * which they subscribed. Subscribers are informed asynchronously one after the next.
     * @param notification A Notification to publish
     * @since 1.3.0
     */
    void publish(Notification notification);

    /**
     * Subscribes to a Notification. Subscribes are automatically notified of all notifications for which they are
     * subscribed.
     * @param notificationClass The notification class to subscribe to
     * @param subscription The Subscription (containing the subscriber) that gets informed
     * @since 1.3.0
     */
    void subscribe(Class<? extends Notification> notificationClass, Subscription subscription);

    /**
     * Subscribes to a Notification. Subscribes are automatically notified of all notifications for which they are
     * subscribed.
     * @param subscription The Subscription (containing the subscriber) that gets informed
     * @since 1.3.0
     */
    void subscribe(Subscription subscription);

    /**
     * Unsubscribes a subscriber. All notifications the subscriber has subscribed to will be
     * unsubscribed. Once unsubscribed, the subscriber will no longer be informed of published
     * notifications.
     * @param subscription The Subscription to unsubscribe.
     *
     * @since 1.3.0
     */
    void unsubscribe(Subscription subscription);

    /**
     * Determines if the specified notification is able to be processed by the NotificationService.
     * If a subscriber exists for the notification, this method will return true, otherwise false.
     * @param notification the notification to query if subscribers exist
     * @return true if the notification has subscribers, false if not
     * @since 1.3.0
     */
    boolean hasSubscriptions(Notification notification);

    /**
     * Shuts down the executioner. Once shut down, future work will not be performed. This should
     * only be called prior to the application being shut down.
     *
     * @since 1.3.0
     */
    void shutdown();

    /**
     * Shuts down this {@link INotificationService}, and waits for already queued notifications
     * to be processed until a given {@code timeout} elapses.
     * <p>
     * Notifications enqueued during shutdown will not be processed.
     *
     * @param timeout The {@link Duration} to wait for notification processing to complete
     * @return {@code true} when all queued notifications were processed prior to {@code timeout} elapsing, otherwise {@code false}
     * @since 2.3.0
     */
    boolean shutdown(final Duration timeout);

}

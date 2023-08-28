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

import java.time.Duration;
import java.util.UUID;

/**
 * Defines an EventService. All event services must be singletons and implement a static
 * getInstance() method.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public interface IEventService {

    /**
     * Publishes events. Published events will get dispatched to all subscribers in the order in which they
     * subscribed. Subscribers are informed asynchronously one after the next.
     * @param event An Event to publish
     *
     * @since 1.2.0
     */
    void publish(Event event);

    /**
     * Subscribes to an event. Subscribes are automatically notified of all events for which they are
     * subscribed.
     * @param eventType The type of event to subscribe to
     * @param subscriberType The Subscriber that gets informed when the type of event is published
     *
     * @since 1.2.0
     */
    void subscribe(Class<? extends Event> eventType, Class<? extends Subscriber> subscriberType);

    /**
     * Unsubscribes a subscriber. All event types the subscriber has subscribed to will be
     * unsubscribed. Once unsubscribed, the subscriber will no longer be informed of published
     * events.
     * @param subscriberType The Subscriber to unsubscribe.
     *
     * @since 1.2.0
     */
    void unsubscribe(Class<? extends Subscriber> subscriberType);

    /**
     * Determines if the specified event is able to be processed by the EventService. If a subscriber
     * exists for the event type, this method will return true, otherwise false.
     * @param event the event to query if subscribers exist
     * @return true if the event has subscribers, false it not
     * @since 1.2.0
     */
    boolean hasSubscriptions(Event event);

    /**
     * Shuts down the executioner. Once shut down, future work will not be performed. This should
     * only be called prior to the application being shut down.
     *
     * @since 1.2.0
     */
    void shutdown();

    /**
     * Shuts down this {@link IEventService}, and waits for already queued events to be processed
     * until a given {@code timeout} elapses.
     * <p>
     * Events enqueued during shutdown will not be processed.
     *
     * @param timeout The {@link Duration} to wait for event processing to complete
     * @return {@code true} when all queued events were processed prior to {@code timeout} elapsing, otherwise {@code false}
     * @since 2.3.0
     */
    boolean shutdown(final Duration timeout);

    /**
     * Determines if the specified event is currently being processed. Processing may indicate the
     * the subscriber task is in the queue and work has not started yet, or may indicate the task
     * is currently being executed. When this event returns false, it does not indicate completion,
     * only that there are no subscriber tasks waiting or working on the specified event.
     * @param event the event to query
     * @return a boolean
     * @since 1.4.0
     */
    boolean isEventBeingProcessed(ChainableEvent event);

    /**
     * Determines if the specified event is currently being processed. Processing may indicate the
     * the subscriber task is in the queue and work has not started yet, or may indicate the task
     * is currently being executed. When this event returns false, it does not indicate completion,
     * only that there are no subscriber tasks waiting or working on the specified event.
     * @param chainIdentifier the UUID of the event to query
     * @return a boolean
     * @since 1.4.0
     */
    boolean isEventBeingProcessed(UUID chainIdentifier);

}


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

}


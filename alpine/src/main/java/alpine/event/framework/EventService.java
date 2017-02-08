/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.event.framework;

import alpine.Config;
import alpine.logging.Logger;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A publish/subscribe (pub/sub) event service that provides the ability to publish events and
 * asynchronously inform all subscribers to subscribed events.
 *
 * @since 1.0.0
 */
public class EventService {

    private static final EventService instance = new EventService();
    private static final Logger logger = Logger.getLogger(EventService.class);
    private Map<Class<? extends Event>, ArrayList<Class<? extends Subscriber>>> subscriptionMap = new ConcurrentHashMap<>();
    private static final ExecutorService executor =
            Executors.newFixedThreadPool(Config.getInstance().getPropertyAsInt(Config.Key.SERVER_EVENT_THREADS));

    private EventService() { }

    public static EventService getInstance() {
        return instance;
    }

    /**
     * Publishes events. Published events will get dispatched to all subscribers in the order in which they
     * subscribed. Subscribers are informed asynchronously one after the next.
     * @param event An Event to publish
     *
     * @since 1.0.0
     */
    public void publish(Event event) {
        logger.debug("Dispatching event: " + event.getClass().toString());
        ArrayList<Class<? extends Subscriber>> subscriberClasses = subscriptionMap.get(event.getClass());
        for (Class clazz: subscriberClasses) {
            logger.debug("Alerting subscriber " + clazz.getName());
            executor.submit(() -> {
                try {
                    Subscriber subscriber = (Subscriber)clazz.newInstance();
                    subscriber.inform(event);
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("An error occurred while informing subscriber: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Subscribes to an event. Subscribes are automatically notified of all events for which they are
     * subscribed.
     * @param eventType The type of event to subscribe to
     * @param subscriberType The Subscriber that gets informed when the type of event is published
     *
     * @since 1.0.0
     */
    public void subscribe(Class<? extends Event> eventType, Class<? extends Subscriber> subscriberType) {
        if (!subscriptionMap.containsKey(eventType)) {
            subscriptionMap.put(eventType, new ArrayList<>());
        }
        ArrayList<Class<? extends Subscriber>> subscribers = subscriptionMap.get(eventType);
        if (!subscribers.contains(subscriberType)) {
            subscribers.add(subscriberType);
        }
    }

    /**
     * Unsubscribes a subscriber. All event types the subscriber has subscribed to will be
     * unsubscribed. Once unsubscribed, the subscriber will no longer be informed of published
     * events.
     * @param subscriberType The Subscriber to unsubscribe.
     *
     * @since 1.0.0
     */
    public void unsubscribe(Class<? extends Subscriber> subscriberType) {
        for (ArrayList<Class<? extends Subscriber>> list : subscriptionMap.values()) {
            list.remove(subscriberType);
        }
    }

    /**
     * Shuts down the executioner. Once shut down, future work will not be performed. This should
     * only be called prior to the application being shut down.
     *
     * @since 1.0.0
     */
    public void shutdown() {
        logger.info("Shutting down EventService");
        executor.shutdown();
    }

}
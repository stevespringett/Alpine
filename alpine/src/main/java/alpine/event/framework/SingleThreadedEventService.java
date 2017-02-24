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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A publish/subscribe (pub/sub) event service that provides the ability to publish events and
 * asynchronously inform all subscribers to subscribed events.
 *
 * This class will use a single thread for the processing of events.
 *
 * Use EventService for an implementation that allows for a configurable number of threads.
 *
 * @since 1.0.0
 */
public class SingleThreadedEventService extends BaseEventService {

    private static final SingleThreadedEventService instance = new SingleThreadedEventService();
    private static final Logger logger = Logger.getLogger(EventService.class);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    static {
        instance.setExecutorService(executor);
        instance.setLogger(logger);
    }

    private SingleThreadedEventService() { }

    public static SingleThreadedEventService getInstance() {
        return instance;
    }

}
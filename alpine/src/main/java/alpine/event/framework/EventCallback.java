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
 * Specifies a Event and an EventService that should process the event as
 * part of a callback.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public class EventCallback {

    private Class<? extends IEventService> eventService;
    private Event event;

    /**
     * Constructs a new EventCallback object. By using this constructor,
     * resolution of the {@link IEventService} to use will be determined by
     * {@link Event#dispatch(Event)}.
     *
     * @param event an Event
     * @since 1.2.0
     */
    public EventCallback(Event event) {
        this.event = event;
    }

    /**
     * Constructs a new EventCallback object.
     * @param eventService the specific IEventService implementation to use
     * @param event an Event
     * @since 1.2.0
     */
    public EventCallback(Class<? extends IEventService> eventService, Event event) {
        this.eventService = eventService;
        this.event = event;
    }

    /**
     * Returns the EventService.
     * @return the EventService
     * @since 1.2.0
     */
    public Class<? extends IEventService> getEventService() {
        return eventService;
    }

    /**
     * Returns the Event.
     * @return the Event
     * @since 1.2.0
     */
    public Event getEvent() {
        return event;
    }

}

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

import java.util.UUID;

/**
 * The Event interface simply defines a 'type'. All Events should implement this
 * interface.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public interface Event {

    /**
     * The dispath method provides convenience in not having to know (or care) about
     * what {@link IEventService} implementation is used to process an event.
     *
     * This method supports both {@link EventService} and {@link SingleThreadedEventService}
     * and may send an event to zero or more of the event services if they have a subscriber
     * capable of processing the event.
     *
     * @param event the event to dispatch
     * @since 1.2.0
     */
    static void dispatch(Event event) {
        boolean informed = false;
        if (EventService.getInstance().hasSubscriptions(event)) {
            informed = true;
            EventService.getInstance().publish(event);
        }
        if (SingleThreadedEventService.getInstance().hasSubscriptions(event)) {
            informed = true;
            SingleThreadedEventService.getInstance().publish(event);
        }
        if (!informed) {
            Logger.getLogger(Event.class).debug("No subscribers to inform from event: " + event.getClass().getName());
        }
    }

    /**
     * This method provides convenience in not having to know (or care) about
     * what {@link IEventService} implementation is used to process an event.
     *
     * This method supports both {@link EventService} and {@link SingleThreadedEventService}.
     *
     * @param event the event to query
     * @return returns true if event is being processed, false if not
     * @since 1.4.0
     */
    static boolean isEventBeingProcessed(ChainableEvent event) {
        return isEventBeingProcessed(event.getChainIdentifier());
    }

    /**
     * This method provides convenience in not having to know (or care) about
     * what {@link IEventService} implementation is used to process an event.
     *
     * This method supports both {@link EventService} and {@link SingleThreadedEventService}.
     *
     * @param chainIdentifier the UUID of the event to query
     * @return returns true if event is being processed, false if not
     * @since 1.4.0
     */
    static boolean isEventBeingProcessed(UUID chainIdentifier) {
        if (EventService.getInstance().isEventBeingProcessed(chainIdentifier)) {
            return true;
        }
        if (SingleThreadedEventService.getInstance().isEventBeingProcessed(chainIdentifier)) {
            return true;
        }
        return false;
    }
}

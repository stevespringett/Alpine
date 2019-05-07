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

import java.util.UUID;

/**
 * The ChainableEvent interface defines methods necessary to support
 * an unbounded chain of callbacks and event between different
 * {@link IEventService} implementations.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public interface ChainableEvent extends Event {

    /**
     * Returns the unique identifier for this event.
     * @return the unique identifier for this event
     * @since 1.4.0
     */
    UUID getEventIdentifier();

    /**
     * Returns the unique identifier for the chain this event is a part of.
     * @return the unique identifier for the chain this event is a part of
     * @since 1.4.0
     */
    UUID getChainIdentifier();

    /**
     * Sets the unique identifier for the chain this event is a part of.
     * @param chainIdentifier the UUID of the chain
     * @since 1.4.0
     */
    void setChainIdentifier(UUID chainIdentifier);

    /**
     * Returns the optional callback event that should be processed if this event is successful.
     * @since 1.2.0
     * @return an Event
     */
    ChainLink[] onSuccess();

    /**
     * Returns the optional callback event that should be processed if this event is not successful.
     * @since 1.2.0
     * @return an Event
     */
    ChainLink[] onFailure();

    /**
     * Fluent method that sets the onSuccess Event and returns this object.
     * @param onSuccessEvent the event to publish if this event succeeds
     * @since 1.2.0
     * @return the current object
     */
    ChainableEvent onSuccess(Event onSuccessEvent);

    /**
     * Fluent method that sets the onSuccess Event and returns this object.
     * @param onSuccessEvent the event to publish if this event succeeds
     * @param onSuccessEventService the specific IEventService implementation to use
     * @since 1.2.0
     * @return the current object
     */
    ChainableEvent onSuccess(Event onSuccessEvent, Class<? extends IEventService> onSuccessEventService);

    /**
     * Fluent method that sets the onFailure Event and returns this object.
     * @param onFailureEvent the event to publish if this event fails
     * @since 1.2.0
     * @return the current object
     */
    ChainableEvent onFailure(Event onFailureEvent);

    /**
     * Fluent method that sets the onFailure Event and returns this object.
     * @param onFailureEvent the event to publish if this event fails
     * @param onFailureEventService the specific IEventService implementation to use
     * @since 1.2.0
     * @return the current object
     */
    ChainableEvent onFailure(Event onFailureEvent, Class<? extends IEventService> onFailureEventService);

}

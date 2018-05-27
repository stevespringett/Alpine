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
 * Provides a default abstract implementation of an Event which supports
 * an unbounded chain of callbacks that can be 'routed' between different
 * {@link IEventService} implementations.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public abstract class AbstractRoutableEvent implements RoutableEvent {

    private Event onSuccessEvent = null;
    private Event onFailureEvent = null;
    private Class<? extends IEventService> onSuccessEventService = null;
    private Class<? extends IEventService> onFailureEventService = null;

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public Event onSuccess() {
        return onSuccessEvent;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public Event onFailure() {
        return onFailureEvent;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public RoutableEvent onSuccess(Event onSuccessEvent) {
        this.onSuccessEvent = onSuccessEvent;
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public RoutableEvent onSuccess(Event onSuccessEvent, Class<? extends IEventService> onSuccessEventService) {
        this.onSuccessEvent = onSuccessEvent;
        this.onSuccessEventService = onSuccessEventService;
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public RoutableEvent onFailure(Event onFailureEvent) {
        this.onFailureEvent = onFailureEvent;
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public RoutableEvent onFailure(Event onFailureEvent, Class<? extends IEventService> onFailureEventService) {
        this.onFailureEvent = onFailureEvent;
        this.onFailureEventService = onFailureEventService;
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    public Class<? extends IEventService> getOnSuccessEventService() {
        return onSuccessEventService;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    public Class<? extends IEventService> getOnFailureEventService() {
        return onFailureEventService;
    }
}

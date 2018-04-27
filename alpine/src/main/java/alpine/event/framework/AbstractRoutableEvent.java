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

    private EventCallback onSuccessCallback = null;
    private EventCallback onFailureCallback = null;

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public EventCallback onSuccess() {
        return onSuccessCallback;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public EventCallback onFailure() {
        return onFailureCallback;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public RoutableEvent onSuccess(EventCallback onSuccessCallback) {
        this.onSuccessCallback = onSuccessCallback;
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public RoutableEvent onFailure(EventCallback onFailureCallback) {
        this.onFailureCallback = onFailureCallback;
        return this;
    }

}

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

import java.util.ArrayList;
import java.util.UUID;

/**
 * Provides a default abstract implementation of an Event which supports
 * an unbounded chain of callbacks that can be routed between different
 * {@link IEventService} implementations.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public abstract class AbstractChainableEvent implements ChainableEvent {

    private final ArrayList<ChainLink> onSuccessChains = new ArrayList<>();
    private final ArrayList<ChainLink> onFailureChains = new ArrayList<>();
    private final UUID eventIdentifier = UUID.randomUUID();
    private UUID chainIdentifier = UUID.randomUUID();

    /**
     * {@inheritDoc}
     * @since 1.4.0
     */
    @Override
    public UUID getEventIdentifier() {
        return eventIdentifier;
    }

    /**
     * {@inheritDoc}
     * @since 1.4.0
     */
    @Override
    public UUID getChainIdentifier() {
        return chainIdentifier;
    }

    /**
     * {@inheritDoc}
     * @since 1.4.0
     */
    public void setChainIdentifier(UUID chainIdentifier) {
        this.chainIdentifier = chainIdentifier;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public ChainLink[] onSuccess() {
        ChainLink[] chain = new ChainLink[onSuccessChains.size()];
        return onSuccessChains.toArray(chain);
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public ChainLink[] onFailure() {
        ChainLink[] chain = new ChainLink[onFailureChains.size()];
        return onFailureChains.toArray(chain);
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public ChainableEvent onSuccess(Event onSuccessEvent) {
        this.onSuccessChains.add(new ChainLink()
                .onSuccess(linkChainIdentifier(onSuccessEvent)));
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public ChainableEvent onSuccess(Event onSuccessEvent, Class<? extends IEventService> onSuccessEventService) {
        this.onSuccessChains.add(new ChainLink()
                .onSuccess(linkChainIdentifier(onSuccessEvent), onSuccessEventService));
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public ChainableEvent onFailure(Event onFailureEvent) {
        this.onFailureChains.add(new ChainLink()
                .onFailure(linkChainIdentifier(onFailureEvent)));
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.2.0
     */
    @Override
    public ChainableEvent onFailure(Event onFailureEvent, Class<? extends IEventService> onFailureEventService) {
        this.onFailureChains.add(new ChainLink()
                .onFailure(linkChainIdentifier(onFailureEvent), onFailureEventService));
        return this;
    }

    /**
     * Assigns the chain identifier for the specified event to the chain identifier
     * value of this instance. This requires the specified event to be an instance
     * of ChainableEvent.
     * @param event the event to chain
     * @return a chained event
     */
    private Event linkChainIdentifier(Event event) {
        if (event instanceof ChainableEvent) {
            ChainableEvent chainableEvent = (ChainableEvent)event;
            chainableEvent.setChainIdentifier(this.getChainIdentifier());
            return chainableEvent;
        }
        return event;
    }

}

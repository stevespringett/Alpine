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

/**
 * Defines the actual success and failure events (and optionally event services),
 * which comprise one or more links in an event chain.
 *
 * @author Steve Springett
 * @since 1.2.0
 */
public final class ChainLink {

    private Event onSuccessEvent = null;
    private Event onFailureEvent = null;
    private Class<? extends IEventService> onSuccessEventService = null;
    private Class<? extends IEventService> onFailureEventService = null;

    public ChainLink onSuccess(Event onSuccessEvent) {
        this.onSuccessEvent = onSuccessEvent;
        return this;
    }

    public ChainLink onSuccess(Event onSuccessEvent, Class<? extends IEventService> onSuccessEventService) {
        this.onSuccessEvent = onSuccessEvent;
        this.onSuccessEventService = onSuccessEventService;
        return this;
    }

    public ChainLink onFailure(Event onFailureEvent) {
        this.onFailureEvent = onFailureEvent;
        return this;
    }

    public ChainLink onFailure(Event onFailureEvent, Class<? extends IEventService> onFailureEventService) {
        this.onFailureEvent = onFailureEvent;
        this.onFailureEventService = onFailureEventService;
        return this;
    }

    public Event getSuccessEvent() {
        return onSuccessEvent;
    }

    public Event getFailureEvent() {
        return onFailureEvent;
    }

    public Class<? extends IEventService> getSuccessEventService() {
        return onSuccessEventService;
    }

    public Class<? extends IEventService> getFailureEventService() {
        return onFailureEventService;
    }
}
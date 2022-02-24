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
 * This class makes it possible to specify event types which should only
 * be processed sequentially (one at a time). This works in conjunction
 * with the {@link #chainIdentifier}. In order for a SingletonCapableEvent
 * to instruct singleton behavior, {@link #isSingleton} should be 'true'
 * and a static chainIdentifier should be specified.
 *
 * @author Steve Springett
 * @since 1.7.0
 */
public abstract class SingletonCapableEvent extends AbstractChainableEvent {

    private boolean isSingleton = false;

    public boolean isSingleton() {
        return isSingleton;
    }

    public void setSingleton(boolean singleton) {
        isSingleton = singleton;
    }

}

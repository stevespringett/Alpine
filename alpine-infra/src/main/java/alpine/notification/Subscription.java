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
package alpine.notification;

public class Subscription {

    private final Class<? extends Subscriber> subscriber;
    private String scope;
    private String group;
    private NotificationLevel level;

    public Subscription(final Class<? extends Subscriber> subscriber) {
        this.subscriber = subscriber;
    }

    public Subscription(final Class<? extends Subscriber> subscriber, final String group) {
        this.subscriber = subscriber;
        this.group = group;
    }

    public Subscription(final Class<? extends Subscriber> subscriber, final NotificationLevel level) {
        this.subscriber = subscriber;
        this.level = level;
    }

    public Subscription(final Class<? extends Subscriber> subscriber, final String scope, final String group, final NotificationLevel level) {
        this.subscriber = subscriber;
        this.scope = scope;
        this.group = group;
        this.level = level;
    }

    public Class<? extends Subscriber> getSubscriber() {
        return subscriber;
    }

    public String getScope() {
        return scope;
    }

    public String getGroup() {
        return group;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof Subscription) {
            final Subscription subscription = (Subscription)object;
            // Validate Scope
            if ((subscription.getScope() != null && !subscription.getScope().equals(this.scope))) {
                return false;
            }
            if ((this.getScope() != null && !subscription.getScope().equals(this.scope))) {
                return false;
            }
            // Validate Group ID
            if ((subscription.getGroup() != null && !subscription.getGroup().equals(this.group))) {
                return false;
            }
            if ((this.getGroup() != null && !subscription.getGroup().equals(this.group))) {
                return false;
            }
            // Validate Level
            if ((subscription.getLevel() != null && subscription.getLevel() != this.level)) {
                return false;
            }
            if ((this.getLevel() != null && subscription.getLevel() != this.level)) {
                return false;
            }
            // Validate Subscriber
            return subscription.getSubscriber() == this.subscriber;
        }
        return false;
    }
}

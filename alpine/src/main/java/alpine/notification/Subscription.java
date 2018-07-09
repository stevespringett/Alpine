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
package alpine.notification;

public class Subscription {

    private Class<? extends Subscriber> subscriber;
    private String scope;
    private String group;
    private NotificationLevel level;

    public Subscription(Class<? extends Subscriber> subscriber) {
        this.subscriber = subscriber;
    }

    public Subscription(Class<? extends Subscriber> subscriber, String group) {
        this.subscriber = subscriber;
        this.group = group;
    }

    public Subscription(Class<? extends Subscriber> subscriber, NotificationLevel level) {
        this.subscriber = subscriber;
        this.level = level;
    }

    public Subscription(Class<? extends Subscriber> subscriber, String scope, String group, NotificationLevel level) {
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
    public boolean equals(Object object) {
        if (object instanceof Subscription) {
            Subscription subscription = (Subscription)object;
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

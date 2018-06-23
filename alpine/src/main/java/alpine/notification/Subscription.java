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
    private String groupId;
    private NotificationType type;

    public Subscription(Class<? extends Subscriber> subscriber) {
        this.subscriber = subscriber;
    }

    public Subscription(Class<? extends Subscriber> subscriber, String groupId) {
        this.subscriber = subscriber;
        this.groupId = groupId;
    }

    public Subscription(Class<? extends Subscriber> subscriber, NotificationType type) {
        this.subscriber = subscriber;
        this.type = type;
    }

    public Subscription(Class<? extends Subscriber> subscriber, String groupId, NotificationType type) {
        this.subscriber = subscriber;
        this.groupId = groupId;
        this.type = type;
    }

    public Class<? extends Subscriber> getSubscriber() {
        return subscriber;
    }

    public String getGroupId() {
        return groupId;
    }

    public NotificationType getType() {
        return type;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Subscription) {
            Subscription subscription = (Subscription)object;
            // Validate Group ID
            if ((subscription.getGroupId() != null && !subscription.getGroupId().equals(this.groupId))) {
                return false;
            }
            if ((this.getGroupId() != null && !subscription.getGroupId().equals(this.groupId))) {
                return false;
            }
            // Validate Type
            if ((subscription.getType() != null && subscription.getType() != this.type)) {
                return false;
            }
            if ((this.getType() != null && subscription.getType() != this.type)) {
                return false;
            }
            // Validate Subscriber
            return subscription.getSubscriber() == this.subscriber;
        }
        return false;
    }
}

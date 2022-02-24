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

import java.time.LocalDateTime;

public final class Notification {

    private String scope;
    private String group;
    private NotificationLevel level;
    private String title;
    private String content;
    private LocalDateTime timestamp;
    private Object subject;

    /**
     * Convenience method provides a shorthand for {@link NotificationService#getInstance}.publish().
     * @param notification the Notification to dispatch
     * @since 1.3.0
     */
    public static void dispatch(final Notification notification) {
        NotificationService.getInstance().publish(notification);
    }

    public Notification() {
        this.timestamp = LocalDateTime.now();
    }

    public Notification scope(final String scope) {
        this.scope = scope;
        return this;
    }

    public Notification scope(final Enum scope) {
        this.scope = scope.name();
        return this;
    }

    public Notification group(final String group) {
        this.group = group;
        return this;
    }

    public Notification group(final Enum group) {
        this.group = group.name();
        return this;
    }

    public Notification level(final NotificationLevel level) {
        this.level = level;
        return this;
    }

    public Notification title(final String title) {
        this.title = title;
        return this;
    }

    public Notification title(final Enum title) {
        this.title = title.name();
        return this;
    }

    public Notification content(final String content) {
        this.content = content;
        return this;
    }

    public Notification content(final Enum content) {
        this.content = content.name();
        return this;
    }

    public Notification timestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Notification subject(final Object subject) {
        this.subject = subject;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public void setLevel(final NotificationLevel level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Object getSubject() {
        return subject;
    }

    public void setSubject(final Object subject) {
        this.subject = subject;
    }
}

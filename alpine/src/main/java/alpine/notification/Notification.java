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

import java.time.LocalDateTime;

public final class Notification {

    private String groupId;
    private NotificationType type;
    private String title;
    private String content;
    private LocalDateTime timestamp;
    private Object subject;

    /**
     * Convenience method provides a shorthand for {@link NotificationService#getInstance().publish()}
     *
     * @author Steve Springett
     * @since 1.3.0
     */
    public static void dispatch(Notification notification) {
        NotificationService.getInstance().publish(notification);
    }

    public Notification() {
        this.timestamp = LocalDateTime.now();
    }

    public Notification groupId(final String groupId) {
        this.groupId = groupId;
        return this;
    }

    public Notification groupId(final Enum groupId) {
        this.groupId = groupId.name();
        return this;
    }

    public Notification type(final NotificationType type) {
        this.type = type;
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Object getSubject() {
        return subject;
    }

    public void setSubject(Object subject) {
        this.subject = subject;
    }
}

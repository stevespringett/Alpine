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
package alpine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Persistable object representing an EventServiceLog.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@PersistenceCapable
public class EventServiceLog implements Serializable {

    private static final long serialVersionUID = -2564458112865683869L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Index(name = "SUBSCRIBERCLASS_IDX")
    @Column(name = "SUBSCRIBERCLASS", allowsNull = "false")
    @NotBlank
    private String subscriberClass;

    @Persistent
    @Column(name = "STARTED")
    private Timestamp started;

    @Persistent
    @Column(name = "COMPLETED")
    private Timestamp completed;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSubscriberClass() {
        return subscriberClass;
    }

    public void setSubscriberClass(String subscriberClass) {
        this.subscriberClass = subscriberClass;
    }

    public Timestamp getStarted() {
        return started;
    }

    public void setStarted(Timestamp started) {
        this.started = started;
    }

    public Timestamp getCompleted() {
        return completed;
    }

    public void setCompleted(Timestamp completed) {
        this.completed = completed;
    }

}

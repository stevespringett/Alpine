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
package alpine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Persistable object representing configuration properties.
 *
 * @author Steve Springett
 * @since 1.3.0
 */
@PersistenceCapable
@Unique(members={"groupName", "propertyName"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigProperty implements Serializable {

    private static final long serialVersionUID = 5286421336166302912L;

    public enum PropertyType {
        BOOLEAN,
        INTEGER,
        NUMBER,
        STRING,
        ENCRYPTEDSTRING,
        TIMESTAMP,
        URL,
        UUID
    }

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent
    @Column(name = "GROUPNAME", allowsNull = "false")
    @NotNull
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The groupName must not contain control characters")
    private String groupName;

    @Persistent
    @Column(name = "PROPERTYNAME", allowsNull = "false")
    @NotNull
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The propertyName must not contain control characters")
    private String propertyName;

    @Persistent
    @Column(name = "PROPERTYVALUE", length = 1024)
    @NotNull
    @Size(min = 1, max = 1024)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The propertyValue must not contain control characters")
    private String propertyValue;

    @Persistent
    @Column(name = "PROPERTYTYPE", jdbcType = "VARCHAR", allowsNull = "false")
    @NotNull
    private PropertyType propertyType;

    @Persistent
    @Column(name = "DESCRIPTION")
    @Size(max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The description must not contain control characters")
    private String description;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}

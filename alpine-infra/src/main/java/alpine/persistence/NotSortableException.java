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
package alpine.persistence;

public class NotSortableException extends IllegalArgumentException {

    private final String resourceName;
    private final String fieldName;
    private final String reason;

    public NotSortableException(final String resourceName, final String fieldName, final String reason) {
        super("Can not sort by %s#%s: %s".formatted(resourceName, fieldName, reason));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.reason = reason;
    }

    @SuppressWarnings("unused")
    public String getResourceName() {
        return resourceName;
    }

    @SuppressWarnings("unused")
    public String getFieldName() {
        return fieldName;
    }

    @SuppressWarnings("unused")
    public String getReason() {
        return reason;
    }

}

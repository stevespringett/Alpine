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
package alpine.server.util;

import alpine.common.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * A collection of useful ObjectMapper methods.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class MapperUtil {

    private static final Logger LOGGER = Logger.getLogger(MapperUtil.class);

    /**
     * Private constructor
     */
    private MapperUtil() { }

    /**
     * Reads in a String value and returns the object for which it represents.
     * @param clazz The expected class of the value
     * @param value the value to parse
     * @param <T> The expected type to return
     * @return the mapped object
     */
    public static <T> T readAsObjectOf(Class<T> clazz, String value) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(value, clazz);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e.fillInStackTrace());
        }
        return null;
    }
}

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
package alpine.util;

import alpine.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class MapperUtil {

    private static final Logger logger = Logger.getLogger(MapperUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T>T readAsObjectOf(Class<T> clazz, String value) {
        try {
            return mapper.readValue(value, clazz);
        } catch (IOException e) {
            logger.error(e.getMessage(), e.fillInStackTrace());
        }
        return null;
    }
}

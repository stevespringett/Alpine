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
package alpine.util;

import org.apache.commons.lang3.StringUtils;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtil {

    /**
     * Private constructor
     */
    private UrlUtil() { }

    /**
     * Ensures that trailing slashes in URLs are removed.
     * @param urlString The URL string to normalize
     * @return a String of the URL without trailing slashes
     * @since 1.6.0
     */
    public static String normalize(String urlString) {
        String s = StringUtils.trimToNull(urlString);
        if (s == null) {
            return null;
        }
        while(s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Ensures that trailing slashes in URLs are removed.
     * @param url The URL to normalize
     * @return a URL without trailing slashes
     * @throws MalformedURLException when a URL is invalid
     */
    public static URL normalize(URL url) throws MalformedURLException {
        return new URL(normalize(url.toExternalForm()));
    }
}

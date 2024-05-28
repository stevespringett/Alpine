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
package alpine.server.filters;

import alpine.Config;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * Adds Powered-By and cache-control headers.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@Priority(Priorities.HEADER_DECORATOR)
public class HeaderFilter implements ContainerResponseFilter {

    private static final String APP_NAME = Config.getInstance().getApplicationName();
    private static final String APP_VERSION = Config.getInstance().getApplicationVersion();
    private static final boolean CORS_ENABLED = Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.CORS_ENABLED);
    private static final String CORS_ALLOW_ORIGIN = Config.getInstance().getProperty(Config.AlpineKey.CORS_ALLOW_ORIGIN);
    private static final String CORS_ALLOW_METHODS = Config.getInstance().getProperty(Config.AlpineKey.CORS_ALLOW_METHODS);
    private static final String CORS_ALLOW_HEADERS = Config.getInstance().getProperty(Config.AlpineKey.CORS_ALLOW_HEADERS);
    private static final String CORS_EXPOSE_HEADERS = Config.getInstance().getProperty(Config.AlpineKey.CORS_EXPOSE_HEADERS);
    private static final boolean CORS_ALLOW_CREDENTIALS = Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.CORS_ALLOW_CREDENTIALS);
    private static final int CORS_MAX_AGE = Config.getInstance().getPropertyAsInt(Config.AlpineKey.CORS_MAX_AGE);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().add("X-Powered-By", APP_NAME + " v" + APP_VERSION);
        responseContext.getHeaders().add(HttpHeaders.CACHE_CONTROL, "private, max-age=0, must-revalidate, no-cache");

        if (CORS_ENABLED) {
            if (StringUtils.isNotBlank(CORS_ALLOW_ORIGIN)) {
                responseContext.getHeaders().add("Access-Control-Allow-Origin", CORS_ALLOW_ORIGIN);
            }
            if (StringUtils.isNotBlank(CORS_ALLOW_METHODS)) {
                responseContext.getHeaders().add("Access-Control-Allow-Methods", CORS_ALLOW_METHODS);
            }
            if (StringUtils.isNotBlank(CORS_ALLOW_HEADERS)) {
                responseContext.getHeaders().add("Access-Control-Allow-Headers", CORS_ALLOW_HEADERS);
            }
            if (StringUtils.isNotBlank(CORS_EXPOSE_HEADERS)) {
                responseContext.getHeaders().add("Access-Control-Expose-Headers", CORS_EXPOSE_HEADERS);
            }
            if (CORS_ALLOW_CREDENTIALS) {
                responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
            }
            if (CORS_MAX_AGE != 0) {
                responseContext.getHeaders().add("Access-Control-Max-Age", CORS_MAX_AGE);
            }
        }
    }
}

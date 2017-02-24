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
package alpine.filters;

import alpine.Config;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;

/**
 * Adds Powered-By, cache-control, and cors headers.
 *
 * @since 1.0.0
 */
@Priority(Priorities.HEADER_DECORATOR)
public class HeaderFilter implements ContainerResponseFilter {

    private String appName;
    private String appVersion;

    private void init() {
        if (appName == null) {
            appName = Config.getInstance().getProperty(Config.AlpineKey.APPLICATION_NAME);
        }
        if (appVersion == null) {
            appVersion = Config.getInstance().getProperty(Config.AlpineKey.APPLICATION_VERSION);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        init();
        responseContext.getHeaders().add("X-Powered-By", appName + " v" + appVersion);
        responseContext.getHeaders().add(HttpHeaders.CACHE_CONTROL, "private, max-age=0, must-revalidate, no-cache");

        // CORS Headers
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET POST PUT DELETE");
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, X-Api-Key, *");
        responseContext.getHeaders().add("Access-Control-Request-Headers", "Origin, Content-Type, Authorization, X-Api-Key, *");
    }

}
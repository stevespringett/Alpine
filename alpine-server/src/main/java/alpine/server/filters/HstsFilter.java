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

import alpine.common.util.BooleanUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 *     Implements HTTP Strict Transport Security (HSTS) (RFC 6797).
 * </p>
 *
 * <p>
 *     This filter is configured via the applications web.xml.
 * </p>
 * <pre>
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;httpsPort&lt;/param-name&gt;
 *         &lt;param-value&gt;443&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;maxAge&lt;/param-name&gt;
 *         &lt;param-value&gt;31536000&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;includeSubdomains&lt;/param-name&gt;
 *         &lt;param-value&gt;false&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * </pre>
 *
 * An example implementation in web.xml:
 *
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;HstsFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;alpine.filters.HstsFilter&lt;/filter-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;httpsPort&lt;/param-name&gt;
 *         &lt;param-value&gt;443&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;maxAge&lt;/param-name&gt;
 *         &lt;param-value&gt;31536000&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;includeSubdomains&lt;/param-name&gt;
 *         &lt;param-value&gt;true&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;HstsFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/&#42;&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class HstsFilter implements Filter {

    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final int DEFAULT_MAX_AGE = 86400;

    private int httpsPort = DEFAULT_HTTPS_PORT;
    private long maxAge = DEFAULT_MAX_AGE;
    private boolean includeSubdomains;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

        final String portString = filterConfig.getInitParameter("httpsPort");
        try {
            httpsPort = Integer.valueOf(portString);
        } catch (NumberFormatException e) {
            httpsPort = DEFAULT_HTTPS_PORT;
        }

        final String maxAgeString = filterConfig.getInitParameter("maxAge");
        try {
            maxAge = Long.valueOf(maxAgeString);
        } catch (NumberFormatException e) {
            maxAge = DEFAULT_MAX_AGE;
        }

        includeSubdomains = BooleanUtil.valueOf(filterConfig.getInitParameter("includeSubdomains"));
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
            throws ServletException, IOException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) resp;

        if (request.isSecure()) {
            if (includeSubdomains) {
                response.setHeader("Strict-Transport-Security", "max-age=" + maxAge + "; includeSubDomains");
            } else {
                response.setHeader("Strict-Transport-Security", "max-age=" + maxAge + ";");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            final StringBuilder sb = new StringBuilder();
            sb.append("https://").append(request.getServerName());

            if (httpsPort != DEFAULT_HTTPS_PORT) {
                sb.append(":").append(httpsPort);
            }
            if (request.getContextPath() != null) {
                sb.append(request.getContextPath());
            }
            if (request.getServletPath() != null) {
                sb.append(request.getServletPath());
            }
            if (request.getPathInfo() != null) {
                sb.append(request.getPathInfo());
            }
            if (request.getQueryString() != null && request.getQueryString().length() > 0) {
                sb.append("?").append(request.getQueryString());
            }
            response.setHeader("Location", sb.toString());
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Intentionally empty to satisfy interface
    }

}

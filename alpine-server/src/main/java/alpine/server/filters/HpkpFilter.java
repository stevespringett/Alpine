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
 *     Implements HTTP Public Key Pinning (RFC 7469).
 * </p>
 *
 * <p>
 *     This filter is configured via the applications web.xml.
 * </p>
 * <pre>
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;primaryHash&lt;/param-name&gt;
 *         &lt;param-value&gt;GRAH5Ex+kB4cCQi5gMU82urf+6kEgbVtzfCSkw55AGk=&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;backupHash&lt;/param-name&gt;
 *         &lt;param-value&gt;lERGk61FITjzyKHcJ89xpc6aDwtRkOPAU0jdnUqzW2s=&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;maxAge&lt;/param-name&gt;
 *         &lt;param-value&gt;31536000&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;includeSubdomains&lt;/param-name&gt;
 *         &lt;param-value&gt;true&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * </pre>
 *
 * An example implementation in web.xml:
 *
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;HpkpFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;alpine.filters.HpkpFilter&lt;/filter-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;primaryHash&lt;/param-name&gt;
 *         &lt;param-value&gt;GRAH5Ex+kB4cCQi5gMU82urf+6kEgbVtzfCSkw55AGk=&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;backupHash&lt;/param-name&gt;
 *         &lt;param-value&gt;lERGk61FITjzyKHcJ89xpc6aDwtRkOPAU0jdnUqzW2s=&lt;/param-value&gt;
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
 *     &lt;filter-name&gt;HpkpFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/&#42;&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class HpkpFilter implements Filter {

    private static final long DEFAULT_MAX_AGE = 15768000; // Default max age is 6 months

    private String primaryHash = null;
    private String backupHash = null;
    private long maxAge = DEFAULT_MAX_AGE;
    private boolean includeSubdomains = false;
    private String reportUri;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

        primaryHash = filterConfig.getInitParameter("primaryHash");
        backupHash = filterConfig.getInitParameter("backupHash");

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
            response.setHeader("Public-Key-Pins", formatPolicy());
        }

        chain.doFilter(request, response);
    }

    /**
     * Formats the HPKP policy header.
     * @return a properly formatted HPKP header
     */
    private String formatPolicy() {
        final StringBuilder sb = new StringBuilder();
        sb.append("pin-sha256").append("=\"").append(primaryHash).append("\"; ");
        sb.append("pin-sha256").append("=\"").append(backupHash).append("\"; ");
        sb.append("max-age").append("=").append(maxAge);

        if (includeSubdomains) {
            sb.append("; ").append("includeSubDomains");
        }

        if (reportUri != null) {
            sb.append("; ").append("report-uri").append("=\"").append(reportUri).append("\"");
        }
        return sb.toString();
    }

    @Override
    public void destroy() {
        // Intentionally empty to satisfy interface
    }
}

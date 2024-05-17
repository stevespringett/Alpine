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

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 *     Implements HTTP Header Field X-Frame-Options (RFC 7034).
 * </p>
 *
 * <p>
 *     This filter is configured via the applications web.xml.
 * </p>
 * <pre>
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;mode&lt;/param-name&gt;
 *         &lt;param-value&gt;DENY&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * </pre>
 *
 * An example implementation in web.xml:
 *
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;ClickjackingFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;alpine.filters.ClickjackingFilter&lt;/filter-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;mode&lt;/param-name&gt;
 *         &lt;param-value&gt;DENY&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;ClickjackingFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/&#42;&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * <p>
 * Valid options are DENY, SAMEORIGIN, or ALLOW-FROM. Use of ALLOW-FROM requires an additional 'uri'
 * parameter to be specified.
 * </p>
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class ClickjackingFilter implements Filter {

    private String mode = "DENY";

    @Override
    public void init(final FilterConfig filterConfig) {
        final String mode = filterConfig.getInitParameter("mode");
        final String uri = filterConfig.getInitParameter("uri");
        if (StringUtils.isNotBlank(mode)) {
            if ("ALLOW-FROM".equals(mode)) {
                this.mode = mode + " " + uri;
            } else if ("DENY".equals(mode) || "SAMEORIGIN".equals(mode)) {
                this.mode = mode;
            }
        }
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletResponse response = (HttpServletResponse) res;
        chain.doFilter(req, response);
        response.addHeader("X-Frame-Options", mode);
    }

    @Override
    public void destroy() {
        // Intentionally empty to satisfy interface
    }

}

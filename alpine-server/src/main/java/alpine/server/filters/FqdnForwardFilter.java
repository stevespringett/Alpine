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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * FqdnForwardFilter is a configurable Servlet Filter that can forward requests made to
 * a hostname or IP address to another host via a 301 redirect. The primary use case for
 * this filter is in conjunction with TLS hostname verification.
 *
 * Sample usage:
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;FqdnForwardFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;alpine.filters.FqdnForwardFilter&lt;/filter-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;host&lt;/param-name&gt;
 *     &lt;param-value&gt;www.example.com&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 *
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;FqdnForwardFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 *
 * </pre>
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class FqdnForwardFilter implements Filter {

    private String host = null;

    /**
     * Initialize "host" parameter from web.xml.
     *
     * @param filterConfig A filter configuration object used by a servlet container
     *                     to pass information to a filter during initialization.
     */
    public void init(final FilterConfig filterConfig) {
        final String host = filterConfig.getInitParameter("host");
        if (StringUtils.isNotBlank(host)) {
            this.host = host;
        }
    }

    /**
     * Forward requests.....
     *
     * @param request The request object.
     * @param response The response object.
     * @param chain Refers to the {@code FilterChain} object to pass control to the next {@code Filter}.
     * @throws IOException a IOException
     * @throws ServletException a ServletException
     */
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

        if (req.getServerName().equals(host)) {
            chain.doFilter(request, response);
            return;
        }

        res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);

        final StringBuilder sb = new StringBuilder();
        sb.append("http");
        if (req.isSecure()) {
            sb.append("s");
        }
        sb.append("://").append(host);
        if (StringUtils.isNotBlank(req.getPathInfo())) {
            sb.append(req.getPathInfo());
        }
        if (StringUtils.isNotBlank(req.getQueryString())) {
            sb.append("?").append(req.getQueryString());
        }
        res.setHeader("Location", sb.toString());
    }


    /**
     * {@inheritDoc}
     */
    public void destroy() {
    }

}
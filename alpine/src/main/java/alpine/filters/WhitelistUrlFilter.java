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
package alpine.filters;

import org.apache.commons.lang3.StringUtils;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * WhitelistUrlFilter is a configurable Servlet Filter that can prevent access to
 * specific URLs. The filter will ignore access to all URLs that are not specifically
 * whitelisted. Ignored URLs result in a HTTP 404 response.
 *
 * The filter may be used when specific files or directories should not be accessible.
 * In the case of executable WARs, use of this filter is highly recommended since
 * executable WARs must meet the requirements of both JAR and WAR files, thus placing
 * compiled classes and their package structure inside the document webroot.
 *
 * Sample usage:
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;WhitelistUrlFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;alpine.filters.WhitelistUrlFilter&lt;/filter-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;allowUrls&lt;/param-name&gt;
 *     &lt;param-value&gt;/images,/css&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 *
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;WhitelistUrlFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 *
 * </pre>
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class WhitelistUrlFilter implements Filter {

    private String[] allowUrls = {};

    /**
     * Initialize "allowUrls" parameter from web.xml.
     *
     * @param filterConfig A filter configuration object used by a servlet container
     *                     to pass information to a filter during initialization.
     */
    public void init(final FilterConfig filterConfig) {

        final String allowParam = filterConfig.getInitParameter("allowUrls");
        if (StringUtils.isNotBlank(allowParam)) {
            this.allowUrls = allowParam.split(",");
        }

    }

    /**
     * Check for allowed URLs being requested.
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

        final String requestUri = req.getRequestURI();
        if (requestUri != null) {
            boolean allowed = false;
            final String requestUrlExcludingContext = requestUri.substring(req.getContextPath().length());
            for (final String url: allowUrls) {
                if (requestUrlExcludingContext.equals("/")) {
                    if (url.trim().equals("/") || (url.trim().equals("/index.jsp")) || (url.trim().equals("/index.html"))) {
                        allowed = true;
                    }
                } else if (requestUrlExcludingContext.startsWith(url.trim())) {
                    allowed = true;
                }
            }
            if (!allowed) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        chain.doFilter(request, response);
    }


    /**
     * {@inheritDoc}
     */
    public void destroy() {
    }

}
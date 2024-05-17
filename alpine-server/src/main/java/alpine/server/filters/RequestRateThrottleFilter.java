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

import alpine.server.util.HttpUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.Stack;

/**
 * The RequestRateThrottleFilter is a Servlet filter that can place a hard limit on the number of requests
 * per second. The filter conforms to RFC-6585 by sending HTTP status code 429 (too many requests) if the
 * limit is exceeded.
 *
 * Sample usage:
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;RequestRateThrottleFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;alpine.filters.RequestRateThrottleFilter&lt;/filter-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;maximumRequestsPerPeriod&lt;/param-name&gt;
 *     &lt;param-value&gt;5&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;timePeriodSeconds&lt;/param-name&gt;
 *     &lt;param-value&gt;10&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 *
 * &lt;!--  Place a request limit on all resources --&gt;
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;RequestRateThrottleFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class RequestRateThrottleFilter implements Filter {

    private int maximumRequestsPerPeriod = 5;
    private int timePeriodSeconds = 10;

    private static final String MAXIMUM_REQUESTS_PER_PERIOD = "maximumRequestsPerPeriod";
    private static final String TIME_PERIOD_SECONDS = "timePeriodSeconds";

    /**
     * {@inheritDoc}
     */
    public void init(final FilterConfig filterConfig) {
        maximumRequestsPerPeriod = Integer.parseInt(filterConfig.getInitParameter(MAXIMUM_REQUESTS_PER_PERIOD));
        timePeriodSeconds = Integer.parseInt(filterConfig.getInitParameter(TIME_PERIOD_SECONDS));
    }

    /**
     * Determines if the request rate is below or has exceeded the the maximum requests per second
     * for the given time period. If exceeded, a HTTP status code of 429 (too many requests) will
     * be send and no further processing of the request will be done. If the request has not exceeded
     * the limit, the request will continue on as normal.
     *
     * @param request a ServletRequest
     * @param response a ServletResponse
     * @param chain a FilterChain
     * @throws IOException a IOException
     * @throws ServletException a ServletException
     */
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        final HttpSession session = httpRequest.getSession(true);

        synchronized (session.getId().intern()) {
            Stack<Date> times = HttpUtil.getSessionAttribute(session, "times");
            if (times == null) {
                times = new Stack<>();
                times.push(new Date(0));
                session.setAttribute("times", times);
            }
            times.push(new Date());
            if (times.size() >= maximumRequestsPerPeriod) {
                times.removeElementAt(0);
            }
            final Date newest = times.get(times.size() - 1);
            final Date oldest = times.get(0);
            final long elapsed = newest.getTime() - oldest.getTime();
            if (elapsed < timePeriodSeconds * 1000) {
                httpResponse.sendError(429);
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
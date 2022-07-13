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
package alpine.server.servlets;

import alpine.Config;
import alpine.common.metrics.Metrics;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @since 2.1.0
 */
public class MetricsServlet extends HttpServlet {

    private boolean metricsEnabled;

    @Override
    public void init() throws ServletException {
        metricsEnabled = Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.METRICS_ENABLED);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        if (metricsEnabled) {
            Metrics.getRegistry().scrape(resp.getWriter());
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}

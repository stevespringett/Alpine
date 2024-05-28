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
import io.prometheus.client.exporter.common.TextFormat;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricsServletTest {

    private Config configMock;
    private HttpServletRequest requestMock;
    private HttpServletResponse responseMock;
    private ByteArrayOutputStream responseOutputStream;
    private PrintWriter responseWriter;

    @Before
    public void setUp() {
        configMock = mock(Config.class);
        requestMock = mock(HttpServletRequest.class);
        responseMock = mock(HttpServletResponse.class);
        responseOutputStream = new ByteArrayOutputStream();
        responseWriter = new PrintWriter(responseOutputStream);
    }

    @Test
    public void shouldRespondWithMetricsWhenEnabled() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.METRICS_ENABLED))).thenReturn(true);

        when(responseMock.getWriter()).thenReturn(responseWriter);

        final var servlet = new MetricsServlet(configMock);
        servlet.init();
        servlet.doGet(requestMock, responseMock);

        verify(responseMock).setStatus(eq(HttpServletResponse.SC_OK));
        verify(responseMock).setHeader(eq(HttpHeaders.CONTENT_TYPE), eq(TextFormat.CONTENT_TYPE_004));
        assertThat(responseOutputStream.toString()).isEmpty();
    }

    @Test
    public void shouldRespondWithNotFoundWhenNotEnabled() throws Exception {
        when(responseMock.getWriter()).thenReturn(responseWriter);

        final var servlet = new MetricsServlet(configMock);
        servlet.init();
        servlet.doGet(requestMock, responseMock);

        verify(responseMock).setStatus(eq(HttpServletResponse.SC_NOT_FOUND));
        assertThat(responseOutputStream.toString()).isEmpty();
    }

    @Test
    public void shouldRespondWithMetricsWhenEnabledAndAuthenticated() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.METRICS_ENABLED))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.METRICS_AUTH_USERNAME))).thenReturn("metrics-user");
        when(configMock.getProperty(eq(Config.AlpineKey.METRICS_AUTH_PASSWORD))).thenReturn("metrics-password");

        when(requestMock.getHeader(eq(HttpHeaders.AUTHORIZATION))).thenReturn("Basic bWV0cmljcy11c2VyOm1ldHJpY3MtcGFzc3dvcmQ");

        when(responseMock.getWriter()).thenReturn(responseWriter);

        final var servlet = new MetricsServlet(configMock);
        servlet.init();
        servlet.doGet(requestMock, responseMock);

        verify(responseMock).setStatus(eq(HttpServletResponse.SC_OK));
        verify(responseMock).setHeader(eq(HttpHeaders.CONTENT_TYPE), eq(TextFormat.CONTENT_TYPE_004));
        assertThat(responseOutputStream.toString()).isEmpty();
    }

    @Test
    public void shouldRespondWithUnauthorizedWhenEnabledAndAuthenticationFailed() throws Exception {
        when(configMock.getPropertyAsBoolean(eq(Config.AlpineKey.METRICS_ENABLED))).thenReturn(true);
        when(configMock.getProperty(eq(Config.AlpineKey.METRICS_AUTH_USERNAME))).thenReturn("metrics-user");
        when(configMock.getProperty(eq(Config.AlpineKey.METRICS_AUTH_PASSWORD))).thenReturn("metrics-password");

        when(requestMock.getHeader(eq(HttpHeaders.AUTHORIZATION))).thenReturn("Basic Zm9vOmJhcg");

        when(responseMock.getWriter()).thenReturn(responseWriter);

        final var servlet = new MetricsServlet(configMock);
        servlet.init();
        servlet.doGet(requestMock, responseMock);

        verify(responseMock).setStatus(eq(HttpServletResponse.SC_UNAUTHORIZED));
        assertThat(responseOutputStream.toString()).isEmpty();
    }

}
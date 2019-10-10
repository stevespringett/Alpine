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

import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Provides a way to gzip the response if the client can accept a gzip response.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@Provider
public class GZipInterceptor implements ReaderInterceptor, WriterInterceptor {

    private final HttpHeaders httpHeaders;

    /**
     * Constructor.
     * @param httpHeaders the The HttpHeaders
     */
    public GZipInterceptor(@Context @NotNull HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        final List<String> header = context.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
        if (header != null && header.contains("gzip")) {
            // DO NOT CLOSE STREAMS
            final InputStream contentInputSteam = context.getInputStream();
            final GZIPInputStream gzipInputStream = new GZIPInputStream(contentInputSteam);
            context.setInputStream(gzipInputStream);
        }
        return context.proceed();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        final List<String> requestHeader = httpHeaders.getRequestHeader(HttpHeaders.ACCEPT_ENCODING);
        if (requestHeader != null && requestHeader.contains("gzip")) {
            // DO NOT CLOSE STREAMS
            final OutputStream contextOutputStream = context.getOutputStream();
            final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(contextOutputStream);
            context.setOutputStream(gzipOutputStream);
            context.getHeaders().add(HttpHeaders.CONTENT_ENCODING, "gzip");
        }
        context.proceed();
    }

}

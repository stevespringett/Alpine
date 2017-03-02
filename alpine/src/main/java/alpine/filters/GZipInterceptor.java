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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
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

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        final List<String> header = context.getHeaders().get("Content-Encoding");
        if (header != null && header.contains("gzip")) {
            context.setInputStream(new GZIPInputStream(context.getInputStream()));
        }
        return context.proceed();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.setOutputStream(new GZIPOutputStream(context.getOutputStream()));
        context.getHeaders().add("Content-Encoding", "gzip");
        context.proceed();
    }

}

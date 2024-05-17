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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.jakarta.rs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jakarta.rs.cfg.ObjectWriterInjector;
import com.fasterxml.jackson.jakarta.rs.cfg.ObjectWriterModifier;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Adds the ability to optionally output pretty printed responses.
 * Requests containing a 'pretty' parameter in the query string
 * will have formatted responses (if a response body is sent).
 *
 * Example: http://hostname/api/version?pretty
 *
 * @author Steve Springett
 * @since 1.0.0
 */
@Provider
public class PrettyPrintFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        final UriInfo uriInfo = reqCtx.getUriInfo();
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        if (queryParameters.containsKey("pretty")) {
            ObjectWriterInjector.set(new IndentingModifier());
        }
    }

    /**
     * Class that overrides the default method responses are sent by programmatically
     * using pretty print only when requested to do so.
     */
    private static class IndentingModifier extends ObjectWriterModifier {

        @Override
        public ObjectWriter modify(EndpointConfigBase<?> endpointConfigBase, MultivaluedMap<String, Object> multivaluedMap,
                                   Object o, ObjectWriter objectWriter, JsonGenerator jsonGenerator) throws IOException {

            jsonGenerator.useDefaultPrettyPrinter();
            return objectWriter;
        }
    }
}

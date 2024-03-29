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
package alpine.server.resources;

import alpine.model.About;
import alpine.server.auth.AuthenticationNotRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Bundled JAX-RS resource that displays the name of the application, version, and build timestamp.
 *
 * @see About
 * @author Steve Springett
 * @since 1.0.0
 */
@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "version")
public final class VersionResource {

    @GET
    @ApiOperation(
            value = "Returns application version information",
            notes = "Returns a simple json object containing the name of the application and the version",
            response = About.class
    )
    @AuthenticationNotRequired
    public Response getVersion() {
        return Response.ok(new GenericEntity<>(new About()) { }).build();
    }

}

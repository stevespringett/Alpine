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
package com.example.resources.v1;

import alpine.auth.AuthenticationNotRequired;
import alpine.auth.Authenticator;
import alpine.auth.JsonWebToken;
import alpine.logging.Logger;
import alpine.resources.AlpineResource;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.owasp.security.logging.SecurityMarkers;
import javax.naming.AuthenticationException;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.Principal;

@Path("/v1/login")
@Api(value = "login")
public class LoginResource extends AlpineResource {

    private static final Logger logger = Logger.getLogger(LoginResource.class);

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Assert login credentials",
            notes = "Upon a successful login, a JSON Web Token will be returned in the response body. This functionality requires authentication to be enabled.",
            response = String.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    @AuthenticationNotRequired
    public Response validateCredentials(@FormParam("username") String username, @FormParam("password") String password) {
        Authenticator auth = new Authenticator(username, password);
        try {
            Principal principal = auth.authenticate();
            if (principal != null) {
                logger.info(SecurityMarkers.SECURITY_AUDIT, "Login succeeded (username: " + username +
                        " / ip address: " + super.getRemoteAddress() + " / agent: " + super.getUserAgent() + ")");

                JsonWebToken jwt = new JsonWebToken();
                String token = jwt.createToken(principal);
                return Response.ok(token).build();
            }
        } catch (AuthenticationException e) {
        }
        logger.warn(SecurityMarkers.SECURITY_AUDIT, "Unauthorized login attempt (username: " + username +
                " / ip address: " + super.getRemoteAddress() + " / agent: " + super.getUserAgent() + ")");
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

}

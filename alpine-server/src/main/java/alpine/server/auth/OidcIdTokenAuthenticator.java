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

package alpine.server.auth;

import alpine.common.logging.Logger;
import alpine.common.util.ProxyUtil;
import alpine.server.cache.CacheManager;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

/**
 * @since 1.10.0
 */
class OidcIdTokenAuthenticator {

    private static final Logger LOGGER = Logger.getLogger(OidcIdTokenAuthenticator.class);
    static final String JWK_SET_CACHE_KEY = "OIDC_JWK_SET";

    private final OidcConfiguration configuration;
    private final String clientId;

    OidcIdTokenAuthenticator(final OidcConfiguration configuration, final String clientId) {
        this.configuration = configuration;
        this.clientId = clientId;
    }

    OidcProfile authenticate(final String idToken, final OidcProfileCreator profileCreator) throws AlpineAuthenticationException {
        final SignedJWT parsedIdToken;
        try {
            parsedIdToken = SignedJWT.parse(idToken);
        } catch (ParseException e) {
            LOGGER.error("Parsing ID token failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
        }

        final JWKSet jwkSet;
        try {
            jwkSet = resolveJwkSet();
        } catch (IOException | ParseException e) {
            LOGGER.error("Resolving JWK set failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final var idTokenValidator = new IDTokenValidator(
                new Issuer(configuration.getIssuer()), new ClientID(clientId),
                parsedIdToken.getHeader().getAlgorithm(), jwkSet);

        final IDTokenClaimsSet claimsSet;
        try {
            claimsSet = idTokenValidator.validate(parsedIdToken, null);
            LOGGER.debug("ID token claims: " + claimsSet.toJSONString());
        } catch (BadJOSEException | JOSEException e) {
            LOGGER.error("ID token validation failed", e);
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
        }

        return profileCreator.create(claimsSet);
    }

    JWKSet resolveJwkSet() throws IOException, ParseException {
        JWKSet jwkSet = CacheManager.getInstance().get(JWKSet.class, JWK_SET_CACHE_KEY);
        if (jwkSet != null) {
            LOGGER.debug("JWK set loaded from cache");
            return jwkSet;
        }

        LOGGER.debug("Fetching JWK set from " + configuration.getJwksUri());
        final URL jwksUrl = configuration.getJwksUri().toURL();

        final var proxyCfg = ProxyUtil.getProxyConfig();
        if (proxyCfg != null && proxyCfg.shouldProxy(jwksUrl)) {
            LOGGER.debug("Using proxy to fetch JWK set");
            jwkSet = JWKSet.load(configuration.getJwksUri().toURL(), 0, 0, 0, proxyCfg.getProxy());
        } else {
            jwkSet = JWKSet.load(jwksUrl);
        }

        LOGGER.debug("Storing JWK set in cache");
        CacheManager.getInstance().put(JWK_SET_CACHE_KEY, jwkSet);
        return jwkSet;
    }

}

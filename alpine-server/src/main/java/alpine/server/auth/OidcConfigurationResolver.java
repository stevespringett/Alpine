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

import alpine.Config;
import alpine.common.logging.Logger;
import alpine.common.util.ProxyConfig;
import alpine.common.util.ProxyUtil;
import alpine.server.cache.CacheManager;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import net.minidev.json.JSONObject;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URL;

/**
 * @since 1.8.0
 */
public class OidcConfigurationResolver {

    private static final OidcConfigurationResolver INSTANCE = new OidcConfigurationResolver(
            Config.getInstance().getPropertyAsBoolean(Config.AlpineKey.OIDC_ENABLED),
            Config.getInstance().getProperty(Config.AlpineKey.OIDC_ISSUER)
    );
    private static final Logger LOGGER = Logger.getLogger(OidcConfigurationResolver.class);
    static final String CONFIGURATION_CACHE_KEY = "OIDC_CONFIGURATION";

    private final boolean oidcEnabled;
    private final String issuer;

    OidcConfigurationResolver(final boolean oidcEnabled, final String issuer) {
        this.oidcEnabled = oidcEnabled;
        this.issuer = issuer;
    }

    public static OidcConfigurationResolver getInstance() {
        return INSTANCE;
    }

    /**
     * Resolve the {@link OidcConfiguration} either from a remote authorization server or from cache.
     *
     * @return The resolved {@link OidcConfiguration} or {@code null}, when resolving was not possible
     */
    @Nullable
    public OidcConfiguration resolve() {
        if (!oidcEnabled) {
            LOGGER.debug("Will not resolve OIDC configuration: OIDC is disabled");
            return null;
        }

        if (issuer == null) {
            LOGGER.error("Cannot resolve OIDC configuration: No issuer provided");
            return null;
        }

        OidcConfiguration configuration = CacheManager.getInstance().get(OidcConfiguration.class, CONFIGURATION_CACHE_KEY);
        if (configuration != null) {
            LOGGER.debug("OIDC configuration loaded from cache");
            return configuration;
        }

        LOGGER.debug("Fetching OIDC configuration from issuer " + issuer);
        try {
            Issuer issuerObject = new Issuer(this.issuer);
            URL configURL = OIDCProviderMetadata.resolveURL(issuerObject);
            HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.GET, configURL);
            final ProxyConfig proxyCfg = ProxyUtil.getProxyConfig();

            if (proxyCfg != null && proxyCfg.shouldProxy(configURL)) {
                httpRequest.setProxy(proxyCfg.getProxy());
            }

            HTTPResponse httpResponse = httpRequest.send();

            if (httpResponse.getStatusCode() != 200) {
                throw new IOException("Couldn't download OpenID Provider metadata from " + configURL +
                        ": Status code " + httpResponse.getStatusCode());
            }

            JSONObject jsonObject = httpResponse.getContentAsJSONObject();

            OIDCProviderMetadata op = OIDCProviderMetadata.parse(jsonObject);

            if (!issuerObject.equals(op.getIssuer())) {
                throw new GeneralException("The returned issuer doesn't match the expected: " + op.getIssuer());
            }

            configuration = new OidcConfiguration();
            configuration.setIssuer(op.getIssuer().getValue());
            configuration.setJwksUri(op.getJWKSetURI());
            configuration.setUserInfoEndpointUri(op.getUserInfoEndpointURI());

            LOGGER.debug("Storing OIDC configuration in cache: " + configuration);
            CacheManager.getInstance().put(CONFIGURATION_CACHE_KEY, configuration);

            return configuration;

        } catch (IOException | GeneralException e) {
            LOGGER.error("Failed to fetch OIDC configuration from issuer " + issuer, e);
            return null;
        }


    }

}

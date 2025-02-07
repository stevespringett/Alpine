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
import alpine.server.util.OidcUtil;

import jakarta.annotation.Nonnull;
import java.security.Principal;
import java.util.ServiceLoader;

/**
 * @since 1.8.0
 */
public class OidcAuthenticationService implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(OidcAuthenticationService.class);

    private final Config config;
    private final OidcConfiguration oidcConfiguration;
    private final OidcIdTokenAuthenticator idTokenAuthenticator;
    private final OidcUserInfoAuthenticator userInfoAuthenticator;
    private final String idToken;
    private final String accessToken;
    private final OidcAuthenticationCustomizer customizer;

    /**
     * @param accessToken The access token acquired by authenticating with an IdP
     * @deprecated Use {@link #OidcAuthenticationService(String, String)} instead
     */
    @Deprecated
    public OidcAuthenticationService(final String accessToken) {
        this(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve(), null, accessToken);
    }

    /**
     * @param idToken     The ID token acquired by authenticating with an IdP
     * @param accessToken The access token acquired by authenticating with an IdP
     * @since 1.10.0
     */
    public OidcAuthenticationService(final String idToken, final String accessToken) {
        this(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve(), idToken, accessToken);
    }

    /**
     * Constructor for unit tests
     */
    OidcAuthenticationService(final Config config,
            final OidcConfiguration oidcConfiguration,
            final String idToken,
            final String accessToken) {
        this(config, oidcConfiguration,
                new OidcIdTokenAuthenticator(oidcConfiguration, config.getProperty(Config.AlpineKey.OIDC_CLIENT_ID)),
                new OidcUserInfoAuthenticator(oidcConfiguration), idToken, accessToken);
    }

    /**
     * Constructor for unit tests
     *
     * @since 1.10.0
     */
    OidcAuthenticationService(final Config config,
            final OidcConfiguration oidcConfiguration,
            final OidcIdTokenAuthenticator idTokenAuthenticator,
            final OidcUserInfoAuthenticator userInfoAuthenticator,
            final String idToken,
            final String accessToken) {
        this.config = config;
        this.oidcConfiguration = oidcConfiguration;
        this.idTokenAuthenticator = idTokenAuthenticator;
        this.userInfoAuthenticator = userInfoAuthenticator;
        this.idToken = idToken;
        this.accessToken = accessToken;

        String customizerClassName = Config.getInstance().getProperty(Config.AlpineKey.OIDC_AUTH_CUSTOMIZER);
        this.customizer = ServiceLoader.load(OidcAuthenticationCustomizer.class)
                .stream()
                .filter(provider -> provider.type().getName().equals(customizerClassName))
                .map(ServiceLoader.Provider::get)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public boolean isSpecified() {
        return OidcUtil.isOidcAvailable(config, oidcConfiguration) && (accessToken != null || idToken != null);
    }

    /**
     * Authenticate a {@link Principal} using the provided credentials.
     * <p>
     * If an ID token is provided, Alpine will validate it and source configured claims from it.
     * <p>
     * If an access token is provided, Alpine will call the IdP's {@code /userinfo} endpoint with it
     * to verify its validity, and source configured claims from the response.
     * <p>
     * If both access token and ID token are provided, the ID token takes precedence.
     * When all configured claims are found in the ID token, {@code /userinfo} won't be requested.
     * When not all claims were found in the ID token, {@code /userinfo} will be requested supplementary.
     *
     * @return An authenticated {@link Principal}
     * @throws AlpineAuthenticationException When authentication failed
     */
    @Nonnull
    @Override
    public Principal authenticate() throws AlpineAuthenticationException {
        final String usernameClaimName = config.getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM);
        if (usernameClaimName == null) {
            LOGGER.error("No username claim has been configured");
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final boolean teamSyncEnabled = config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION);
        final String teamsClaimName = config.getProperty(Config.AlpineKey.OIDC_TEAMS_CLAIM);

        if (teamSyncEnabled && teamsClaimName == null) {
            LOGGER.error("Team synchronization is enabled, but no teams claim has been configured");
            throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
        }

        final OidcProfileCreator profileCreator = claims -> {
            return customizer.createProfile(claims);
        };

        OidcProfile idTokenProfile = null;
        if (idToken != null) {
            idTokenProfile = idTokenAuthenticator.authenticate(idToken, profileCreator);
            LOGGER.debug("ID token profile: " + idTokenProfile);

            if (customizer.isProfileComplete(idTokenProfile, teamSyncEnabled)) {
                LOGGER.debug("ID token profile is complete, proceeding to authenticate");
                return customizer.onAuthenticationSuccess(idTokenProfile);
            }
        }

        OidcProfile userInfoProfile = null;
        if (accessToken != null) {
            userInfoProfile = userInfoAuthenticator.authenticate(accessToken, profileCreator);
            LOGGER.debug("UserInfo profile: " + userInfoProfile);

            if (customizer.isProfileComplete(userInfoProfile, teamSyncEnabled)) {
                LOGGER.debug("UserInfo profile is complete, proceeding to authenticate");
                return customizer.onAuthenticationSuccess(userInfoProfile);
            }
        }

        OidcProfile mergedProfile = null;
        if (idTokenProfile != null && userInfoProfile != null) {
            mergedProfile = customizer.mergeProfiles(idTokenProfile, userInfoProfile);
            LOGGER.debug("Merged profile: " + mergedProfile);

            if (customizer.isProfileComplete(mergedProfile, teamSyncEnabled)) {
                LOGGER.debug("Merged profile is complete, proceeding to authenticate");
                return customizer.onAuthenticationSuccess(mergedProfile);
            }
        }

        LOGGER.error("Unable to assemble complete profile (ID token: %s, UserInfo: %s, Merged: %s)"
                .formatted(idTokenProfile, userInfoProfile, mergedProfile));

        throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.OTHER);
    }

}

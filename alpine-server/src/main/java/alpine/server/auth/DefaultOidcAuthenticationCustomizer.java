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

import net.minidev.json.JSONObject;

import alpine.Config;
import alpine.common.logging.Logger;
import alpine.model.OidcUser;
import alpine.persistence.AlpineQueryManager;
import alpine.server.util.OidcUtil;

import java.util.List;
import java.util.Objects;

import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

public class DefaultOidcAuthenticationCustomizer implements OidcAuthenticationCustomizer {

    private static final Logger LOGGER = Logger.getLogger(DefaultOidcAuthenticationCustomizer.class);

    private final Config config;
    private final OidcConfiguration oidcConfiguration;
    private final String idToken;
    private final String accessToken;

    public DefaultOidcAuthenticationCustomizer(final String idToken, final String accessToken) {
        this(Config.getInstance(), OidcConfigurationResolver.getInstance().resolve(), idToken, accessToken);
    }

    DefaultOidcAuthenticationCustomizer(final Config config,
            final OidcConfiguration oidcConfiguration,
            final String idToken,
            final String accessToken) {
        this.config = config;
        this.oidcConfiguration = oidcConfiguration;
        this.idToken = idToken;
        this.accessToken = accessToken;
    }

    private OidcUser autoProvision(final AlpineQueryManager qm, final OidcProfile profile) {
        var user = new OidcUser();
        user.setUsername(profile.getUsername());
        user.setSubjectIdentifier(profile.getSubject());
        user.setEmail(profile.getEmail());
        user = qm.persist(user);

        if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)) {
            LOGGER.debug("Synchronizing teams for user " + user.getUsername());
            return qm.synchronizeTeamMembership(user, profile.getGroups());
        }

        final List<String> defaultTeams = config.getPropertyAsList(Config.AlpineKey.OIDC_TEAMS_DEFAULT);
        if (!defaultTeams.isEmpty()) {
            LOGGER.debug("Assigning default teams %s to user %s".formatted(defaultTeams, user.getUsername()));
            return qm.addUserToTeams(user, defaultTeams);
        }

        return user;
    }

    public OidcProfile createProfile(ClaimsSet claimsSet) {
        final String teamsClaimName = config.getProperty(Config.AlpineKey.OIDC_TEAMS_CLAIM);
        final String usernameClaimName = config.getProperty(Config.AlpineKey.OIDC_USERNAME_CLAIM);
        final var profile = new OidcProfile();

        profile.setSubject(claimsSet.getStringClaim(UserInfo.SUB_CLAIM_NAME));
        profile.setUsername(claimsSet.getStringClaim(usernameClaimName));
        profile.setGroups(claimsSet.getStringListClaim(teamsClaimName));
        profile.setEmail(claimsSet.getStringClaim(UserInfo.EMAIL_CLAIM_NAME));

        JSONObject claimsObj = claimsSet.toJSONObject();
        claimsObj.remove(UserInfo.EMAIL_CLAIM_NAME);
        claimsObj.remove(UserInfo.SUB_CLAIM_NAME);
        claimsObj.remove(teamsClaimName);
        claimsObj.remove(usernameClaimName);

        profile.setCustomValues(claimsObj);

        return profile;
    }

    public boolean isProfileComplete(final OidcProfile profile, final boolean teamSyncEnabled) {
        return profile.getSubject() != null && profile.getUsername() != null
                && (!teamSyncEnabled || (profile.getGroups() != null));
    }

    public boolean isSpecified() {
        return OidcUtil.isOidcAvailable(config, oidcConfiguration) && (accessToken != null || idToken != null);
    }

    public OidcProfile mergeProfiles(final OidcProfile left, final OidcProfile right) {
        final var profile = new OidcProfile();

        profile.setSubject(selectProfileClaim(left.getSubject(), right.getSubject()));
        profile.setUsername(selectProfileClaim(left.getUsername(), right.getUsername()));
        profile.setGroups(selectProfileClaim(left.getGroups(), right.getGroups()));
        profile.setEmail(selectProfileClaim(left.getEmail(), right.getEmail()));

        JSONObject customValues = left.getCustomValues();
        customValues.merge(right.getCustomValues());
        profile.setCustomValues(customValues);

        return profile;
    }

    public OidcUser onAuthenticationSuccess(OidcProfile profile) throws AlpineAuthenticationException {
        try (final var qm = new AlpineQueryManager()) {
            OidcUser user = qm.getOidcUser(profile.getUsername());

            if (user != null) {
                LOGGER.debug("Attempting to authenticate user: " + user.getUsername());

                if (user.getSubjectIdentifier() == null) {
                    LOGGER.debug("Assigning subject identifier %s to user %s"
                            .formatted(profile.getSubject(), user.getUsername()));

                    user.setSubjectIdentifier(profile.getSubject());
                    user.setEmail(profile.getEmail());

                    return qm.updateOidcUser(user);
                } else if (!user.getSubjectIdentifier().equals(profile.getSubject())) {
                    LOGGER.error("Refusing to authenticate user %s: subject identifier has changed (%s to %s)"
                            .formatted(user.getUsername(), user.getSubjectIdentifier(), profile.getSubject()));

                    throw new AlpineAuthenticationException(
                            AlpineAuthenticationException.CauseType.INVALID_CREDENTIALS);
                }

                if (!Objects.equals(user.getEmail(), profile.getEmail())) {
                    LOGGER.debug("Updating email of user %s: %s -> %s"
                            .formatted(user.getUsername(), user.getEmail(), profile.getEmail()));

                    user.setEmail(profile.getEmail());
                    user = qm.updateOidcUser(user);
                }

                if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_TEAM_SYNCHRONIZATION)) {
                    return qm.synchronizeTeamMembership(user, profile.getGroups());
                }

                return user;
            } else if (config.getPropertyAsBoolean(Config.AlpineKey.OIDC_USER_PROVISIONING)) {
                LOGGER.debug("The user (%s) authenticated successfully but the account has not been provisioned"
                        .formatted(profile.getUsername()));

                return autoProvision(qm, profile);
            } else {
                LOGGER.debug("The user (%s) is unmapped and user provisioning is not enabled"
                        .formatted(profile.getUsername()));

                throw new AlpineAuthenticationException(AlpineAuthenticationException.CauseType.UNMAPPED_ACCOUNT);
            }
        }
    }

    private <T> T selectProfileClaim(final T left, final T right) {
        return (left != null) ? left : right;
    }

}

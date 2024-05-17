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
import alpine.model.LdapUser;
import alpine.model.OidcUser;
import alpine.model.Permission;
import alpine.security.crypto.KeyManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.owasp.security.logging.SecurityMarkers;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Decouples the general usage of JSON Web Tokens with the actual implementation of a JWT library
 * All JWT usages should only go through this class and hide the actual implementation details
 * and to avoid improper or insecure use of JWTs.
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public class JsonWebToken {

    private static final Logger LOGGER = Logger.getLogger(JsonWebToken.class);
    private static final String IDENTITY_PROVIDER_CLAIM = "idp";
    private static String ISSUER = "Alpine";
    static {
        if (Config.getInstance().getApplicationName() != null) {
            ISSUER = Config.getInstance().getApplicationName();
        } else {
            Config.getInstance().getFrameworkName();
        }
    }

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private String subject;
    private Date expiration;
    private IdentityProvider identityProvider;

    /**
     * Constructs a new JsonWebToken object using the default Alpine-generated
     * secret key.
     *
     * @see KeyManager#getSecretKey()
     * @since 1.0.0
     */
    public JsonWebToken() {
        this.privateKey = KeyManager.getInstance().getPrivateKey();
        this.publicKey = KeyManager.getInstance().getPublicKey();
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param principal the Principal to create the token for
     * @return a String representation of the generated token
     * @since 1.0.0
     */
    public String createToken(final Principal principal) {
        return createToken(principal, null);
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param principal the Principal to create the token for
     * @param permissions the effective list of permissions for the principal
     * @return a String representation of the generated token
     * @since 1.1.0
     */
    public String createToken(final Principal principal, final List<Permission> permissions) {
        return createToken(principal, permissions, null);
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param principal the Principal to create the token for
     * @param permissions the effective list of permissions for the principal
     * @param identityProvider the identity provider the principal was authenticated with. If null, it will be derived from principal
     * @return a String representation of the generated token
     * @since 1.8.0
     */
    public String createToken(final Principal principal, final List<Permission> permissions, final IdentityProvider identityProvider) {
        final Date today = new Date();
        final JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.subject(principal.getName());
        jwtBuilder.issuer(ISSUER);
        jwtBuilder.issuedAt(today);
        jwtBuilder.expiration(addDays(today, 7));
        if (permissions != null) {
            jwtBuilder.claim("permissions", permissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.joining(","))
            );
        }
        if (identityProvider != null) {
            jwtBuilder.claim(IDENTITY_PROVIDER_CLAIM, identityProvider.name());
        } else {
            if (principal instanceof LdapUser) {
                jwtBuilder.claim(IDENTITY_PROVIDER_CLAIM, IdentityProvider.LDAP.name());
            } else if (principal instanceof OidcUser) {
                jwtBuilder.claim(IDENTITY_PROVIDER_CLAIM, IdentityProvider.OPENID_CONNECT.name());
            } else {
                jwtBuilder.claim(IDENTITY_PROVIDER_CLAIM, IdentityProvider.LOCAL.name());
            }
        }
        return jwtBuilder.signWith(privateKey).compact();
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param claims a Map of all claims
     * @return a String representation of the generated token
     * @since 1.0.0
     */
    public String createToken(final Map<String, Object> claims) {
        final JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.claims(claims);
        return jwtBuilder.signWith(privateKey).compact();
    }

    /**
     * Validates a JWT by ensuring the signature matches and validates
     * against the SecretKey and checks the expiration date.
     *
     * @param token the token to validate
     * @return true if validation successful, false if not
     * @since 1.0.0
     */
    public boolean validateToken(final String token) {
        try {
            final JwtParser jwtParser = Jwts.parser().verifyWith(publicKey).build();
            final Jws<Claims> claims = jwtParser.parseSignedClaims(token);
            this.subject = claims.getPayload().getSubject();
            this.expiration = claims.getPayload().getExpiration();
            this.identityProvider = IdentityProvider.forName(claims.getPayload().get(IDENTITY_PROVIDER_CLAIM, String.class));
            return true;
        } catch (SignatureException e) {
            LOGGER.info(SecurityMarkers.SECURITY_FAILURE, "Received token that did not pass signature verification");
        } catch (ExpiredJwtException e) {
            LOGGER.debug(SecurityMarkers.SECURITY_FAILURE, "Received expired token");
        } catch (MalformedJwtException e) {
            LOGGER.debug(SecurityMarkers.SECURITY_FAILURE, "Received malformed token");
            LOGGER.debug(SecurityMarkers.SECURITY_FAILURE, e.getMessage());
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            LOGGER.error(SecurityMarkers.SECURITY_FAILURE, e.getMessage());
        }
        return false;
    }

    /**
     * Create a new future Date from the specified Date.
     *
     * @param date The date to base the future date from
     * @param days The number of dates to + offset
     * @return a future date
     */
    private Date addDays(final Date date, final int days) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }

    /**
     * Returns the subject of the token.
     * @return a String
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the expiration of the token.
     * @return a Date
     */
    public Date getExpiration() {
        return expiration;
    }

    /**
     * Returns the identity provider of the token.
     * @return an IdentityProvider
     */
    public IdentityProvider getIdentityProvider() {
        return identityProvider;
    }

}

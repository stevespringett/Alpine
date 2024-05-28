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
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.owasp.security.logging.SecurityMarkers;

import javax.crypto.SecretKey;
import java.security.Principal;
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

    private final SecretKey key;
    private String subject;
    private Date expiration;
    private IdentityProvider identityProvider;

    /**
     * Constructs a new JsonWekToken object using the specified SecretKey which can
     * be retrieved from {@link KeyManager#getSecretKey()} to use the Alpine-generated
     * secret key. Usage of other SecretKeys is allowed but management of those keys
     * is up to the implementor.
     *
     * @param key the SecretKey to use in generating or validating the token
     * @since 1.0.0
     */
    public JsonWebToken(final SecretKey key) {
        // NB: JJWT will throw if the key's algorithm is not explicitly any of: HmacSHA512, HmacSHA384, or HmacSHA256.
        // Alpine generates its secret key with algorithm AES per default.
        // Keys#hmacShaKeyFor will pick the correct HmacSHA* algorithm based on the key's bit length.
        this.key = Keys.hmacShaKeyFor(key.getEncoded());
    }

    /**
     * Constructs a new JsonWebToken object using the default Alpine-generated
     * secret key.
     *
     * @see KeyManager#getSecretKey()
     * @since 1.0.0
     */
    public JsonWebToken() {
        this(KeyManager.getInstance().getSecretKey());
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
    public String createToken(final Principal principal, final List<Permission> permissions,
            final IdentityProvider identityProvider) {
        final int ttl = Config.getInstance().getPropertyAsInt(Config.AlpineKey.AUTH_JWT_TTL_SECONDS);
        return createToken(principal, permissions, identityProvider, ttl);
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param principal the Principal to create the token for
     * @param permissions the effective list of permissions for the principal
     * @param identityProvider the identity provider the principal was authenticated with. If null, it will be derived from principal
     * @param ttlSeconds the token time-to-live in seconds
     * @return a String representation of the generated token
     * @since 3.0.0
     */
    public String createToken(final Principal principal, final List<Permission> permissions, final IdentityProvider identityProvider, final int ttlSeconds) {
        final Date now = new Date();
        final JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.subject(principal.getName());
        jwtBuilder.issuer(ISSUER);
        jwtBuilder.issuedAt(now);
        jwtBuilder.expiration(addSeconds(now, ttlSeconds));
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
        return jwtBuilder.signWith(key).compact();
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
        return jwtBuilder.signWith(key).compact();
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
            final JwtParser jwtParser = Jwts.parser().verifyWith(key).build();
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
     * @param date    The date to base the future date from
     * @param seconds The number of seconds to + offset
     * @return a future date
     */
    private Date addSeconds(final Date date, final int seconds) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.SECOND, seconds); //minus number would decrement the seconds
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

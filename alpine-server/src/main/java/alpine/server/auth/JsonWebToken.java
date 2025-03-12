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
import java.util.Objects;
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
    private static final String PERMISSIONS_CLAIM = "permissions";
    private static String ISSUER = Config.getInstance().getApplicationName();
    static {
        ISSUER = ISSUER != null ? ISSUER : Config.getInstance().getFrameworkName();
    }

    private final SecretKey key;
    private String subject;
    private int ttl = Config.getInstance().getPropertyAsInt(Config.AlpineKey.AUTH_JWT_TTL_SECONDS);
    private Date expiration;
    private IdentityProvider identityProvider;
    private List<Permission> permissions;
    private Map<String, Object> extraClaims;

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
     * Finalize and generate the encoded String representation of the JsonWebToken.
     *
     * @return a String representation of the generated token
     * @since 3.2.0
     */
    public String build() {
        final Date now = new Date();
        this.expiration = addSeconds(now, ttl);

        return Jwts.builder().subject(subject)
                .issuedAt(now)
                .issuer(ISSUER)
                .expiration(expiration)
                .claim(PERMISSIONS_CLAIM, this.permissions == null ? null
                        : permissions.stream()
                                .map(Permission::getName)
                                .collect(Collectors.joining(",")))
                .claim(IDENTITY_PROVIDER_CLAIM, identityProvider)
                .claims(extraClaims)
                .signWith(key)
                .compact();
    }

    /**
     * Builder method to set the value of the {@code exp} claim.
     *
     * @param date the expiration date
     * @return the updated JsonWebToken object
     * @since 3.2.0
     */
    public JsonWebToken expiration(Date date) {
        this.expiration = date;
        return this;
    }

    /**
     * Builder method to set claims explicitly.
     *
     * @param extraClaims a Map of claims to set
     * @return the updated JsonWebToken object
     * @since 3.2.0
     */
    public JsonWebToken extraClaims(Map<String, Object> extraClaims) {
        this.extraClaims = extraClaims;
        return this;
    }

    /**
     * Builder method to set the value of the {@code idp} claim.
     *
     * @param identityProvider the {@link IdentityProvider} to set
     * @return the updated JsonWebToken object
     * @since 3.2.0
     */
    public JsonWebToken identityProvider(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
        return this;
    }

    /**
     * Builder method to set the value of the {@code permissions} claim.
     *
     * @param permissions the list of {@link Permission}s to set
     * @return the updated JsonWebToken object
     * @since 3.2.0
     */
    public JsonWebToken permissions(List<Permission> permissions) {
        this.permissions = permissions;
        return this;
    }

    /**
     * Builder method to set the value of the {@code sub} claim.
     *
     * @param subject the subject of the token
     * @return the updated JsonWebToken object
     * @since 3.2.0
     */
    public JsonWebToken subject(String subject) {
        this.subject = subject;
        return this;
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
    public String createToken(
            final Principal principal,
            final List<Permission> permissions,
            final IdentityProvider identityProvider) {
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
    public String createToken(
            final Principal principal,
            final List<Permission> permissions,
            final IdentityProvider identityProvider,
            final int ttlSeconds) {
        return createToken(principal, permissions, identityProvider, ttlSeconds, null);
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param principal the Principal to create the token for
     * @param permissions the effective list of permissions for the principal
     * @param identityProvider the identity provider the principal was authenticated with. If null, it will be derived from principal
     * @param ttlSeconds the token time-to-live in seconds
     * @param extraClaims map of additional claims to include
     * @return a String representation of the generated token
     * @since 3.2.0
     */
    public String createToken(
            final Principal principal,
            final List<Permission> permissions,
            final IdentityProvider identityProvider,
            final int ttlSeconds,
            final Map<String, Object> extraClaims) {
        this.ttl = ttlSeconds;

        return this.subject(principal.getName())
                .permissions(permissions)
                .identityProvider(Objects.requireNonNullElse(identityProvider, switch (principal) {
                    case LdapUser user -> IdentityProvider.LDAP;
                    case OidcUser user -> IdentityProvider.OPENID_CONNECT;
                    default -> IdentityProvider.LOCAL;
                }))
                .extraClaims(extraClaims)
                .build();
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
        return Jwts.builder()
                .claims(claims)
                .signWith(key)
                .compact();
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
            final Claims payload = claims.getPayload();

            this.subject(payload.getSubject())
                    .expiration(payload.getExpiration())
                    .identityProvider(IdentityProvider.forName(payload.get(IDENTITY_PROVIDER_CLAIM, String.class)));

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
        cal.add(Calendar.SECOND, seconds); // minus number would decrement the seconds
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

    /**
     * Returns the permissions of the token.
     * @return list of {@link Permission}s
     */
    public List<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Returns the extra claims of the token.
     * @return map of claims
     */
    public Map<String, Object> getExtraClaims() {
        return extraClaims;
    }

}

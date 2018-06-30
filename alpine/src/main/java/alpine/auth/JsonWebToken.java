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
package alpine.auth;

import alpine.Config;
import alpine.crypto.KeyManager;
import alpine.logging.Logger;
import alpine.model.Permission;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.owasp.security.logging.SecurityMarkers;
import javax.crypto.SecretKey;
import java.security.Key;
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
    private static String ISSUER = "Alpine";
    static {
        if (Config.getInstance().getApplicationName() != null) {
            ISSUER = Config.getInstance().getApplicationName();
        } else {
            Config.getInstance().getFrameworkName();
        }
    }

    private Key key;
    private String subject;
    private Date expiration;

    /**
     * Constructs a new JsonWekToken object using the specified SecretKey which can
     * be retrieved from {@link KeyManager#getSecretKey()} to use the Alpine-generated
     * secret key. Usage of other SecretKeys is allowed but management of those keys
     * is up to the implementor.
     *
     * @param key the SecretKey to use in generating or validating the token
     * @since 1.0.0
     */
    public JsonWebToken(SecretKey key) {
        this.key = key;
    }

    /**
     * Constructs a new JsonWebToken object using the default Alpine-generated
     * secret key.
     *
     * @see KeyManager#getSecretKey()
     * @since 1.0.0
     */
    public JsonWebToken() {
        this.key = KeyManager.getInstance().getSecretKey();
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param principal the Principal to create the token for
     * @return a String representation of the generated token
     * @since 1.0.0
     */
    public String createToken(Principal principal) {
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
    public String createToken(Principal principal, List<Permission> permissions) {
        final Date today = new Date();
        final JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.setSubject(principal.getName());
        jwtBuilder.setIssuer(ISSUER);
        jwtBuilder.setIssuedAt(today);
        jwtBuilder.setExpiration(addDays(today, 7));
        if (permissions != null) {
            jwtBuilder.claim("permissions", permissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.joining(","))
            );
        }
        return jwtBuilder.signWith(SignatureAlgorithm.HS256, key).compact();
    }

    /**
     * Creates a new JWT for the specified principal. Token is signed using
     * the SecretKey with an HMAC 256 algorithm.
     *
     * @param claims a Map of all claims
     * @return a String representation of the generated token
     * @since 1.0.0
     */
    public String createToken(Map<String, Object> claims) {
        final JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.setClaims(claims);
        return jwtBuilder.signWith(SignatureAlgorithm.HS256, key).compact();
    }

    /**
     * Validates a JWT by ensuring the signature matches and validates
     * against the SecretKey and checks the expiration date.
     *
     * @param token the token to validate
     * @return true if validation successful, false if not
     * @since 1.0.0
     */
    public boolean validateToken(String token) {
        try {
            final JwtParser jwtParser = Jwts.parser().setSigningKey(key);
            jwtParser.parse(token);
            this.subject = jwtParser.parseClaimsJws(token).getBody().getSubject();
            this.expiration = jwtParser.parseClaimsJws(token).getBody().getExpiration();
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
    private Date addDays(Date date, int days) {
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

}

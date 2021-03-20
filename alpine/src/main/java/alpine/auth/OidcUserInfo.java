package alpine.auth;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/**
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse">OpenID Connect specification: UserInfo Response</a>
 * @since 1.8.0
 */
public class OidcUserInfo {

    private final Map<String, Object> claims;

    OidcUserInfo() {
        claims = new HashMap<>();
    }

    public String getSubject() {
        return getClaim("sub", String.class);
    }

    public String getEmail() {
        return getClaim("email", String.class);
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    @SuppressWarnings("unchecked")
    public <T> T getClaim(final String key, final Class<T> clazz) {
        final Object claim = claims.get(key);

        if (claim != null) {
            return clazz.cast(claim);
        }

        return null;
    }

    @JsonAnySetter
    public void setClaim(final String key, final Object value) {
        this.claims.put(key, value);
    }

}

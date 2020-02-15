package alpine.auth;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class OidcUserInfo {

    @JsonProperty("sub")
    private String subject;

    @JsonProperty("email")
    private String email;

    private Map<String, Object> claims;

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @JsonAnyGetter
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

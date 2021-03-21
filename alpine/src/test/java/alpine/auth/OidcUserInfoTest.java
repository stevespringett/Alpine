package alpine.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcUserInfoTest {

    // No serialization test: OidcUserInfo is only used internally by Alpine and is never serialized, only deserialized

    @Test
    @SuppressWarnings("unchecked")
    public void testJsonDeserialization() throws JsonProcessingException {
        final String userInfoJson = "" +
                "{\n" +
                "    \"sub\": \"666\",\n" +
                "    \"name\": \"user\",\n" +
                "    \"nickname\": \"user666\",\n" +
                "    \"email\": \"user@example.com\",\n" +
                "    \"email_verified\": true,\n" +
                "    \"groups\": [\"groupName1\",\"groupName2\"]\n" +
                "}";

        final OidcUserInfo userInfo = new ObjectMapper().readValue(userInfoJson, OidcUserInfo.class);
        assertThat(userInfo.getSubject()).isEqualTo("666");
        assertThat(userInfo.getEmail()).isEqualTo("user@example.com");
        assertThat(userInfo.getClaim("sub", String.class)).isEqualTo("666");
        assertThat(userInfo.getClaim("email", String.class)).isEqualTo("user@example.com");
        assertThat(userInfo.getClaim("nickname", String.class)).isEqualTo("user666");
        assertThat(userInfo.getClaim("email_verified", Boolean.class)).isTrue();
        assertThat(userInfo.getClaim("groups", List.class)).containsExactly("groupName1", "groupName2");
    }

}
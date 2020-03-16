package alpine.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcGroupTest {

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        final OidcGroup oidcGroup = new OidcGroup();
        oidcGroup.setId(666);
        oidcGroup.setUuid(UUID.fromString("658c7f29-7286-47c4-8d37-527d4a6c0317"));
        oidcGroup.setName("groupName");

        assertThat(new ObjectMapper().writeValueAsString(oidcGroup)).isEqualTo("" +
                "{" +
                "\"uuid\":\"658c7f29-7286-47c4-8d37-527d4a6c0317\"," +
                "\"name\":\"groupName\"" +
                "}");
    }

    @Test
    public void testJsonDeserialization() throws JsonProcessingException {
        final OidcGroup oidcGroup = new ObjectMapper().readValue("" +
                "{" +
                "\"id\":666," +
                "\"uuid\":\"658c7f29-7286-47c4-8d37-527d4a6c0317\"," +
                "\"name\":\"groupName\"" +
                "}", OidcGroup.class);

        assertThat(oidcGroup.getId()).isZero();
        assertThat(oidcGroup.getUuid()).isEqualTo(UUID.fromString("658c7f29-7286-47c4-8d37-527d4a6c0317"));
        assertThat(oidcGroup.getName()).isEqualTo("groupName");
    }

}
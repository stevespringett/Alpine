package alpine.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MappedOidcGroupTest {

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        final Team team = new Team();
        team.setName("teamName");

        final OidcGroup oidcGroup = new OidcGroup();
        oidcGroup.setName("groupName");

        final MappedOidcGroup mappedOidcGroup = new MappedOidcGroup();
        mappedOidcGroup.setId(666);
        mappedOidcGroup.setTeam(team);
        mappedOidcGroup.setGroup(oidcGroup);
        mappedOidcGroup.setUuid(UUID.fromString("6e394949-9988-4459-85e9-feda224ac321"));

        assertThat(new ObjectMapper().writeValueAsString(mappedOidcGroup)).isEqualTo("" +
                "{" +
                "\"group\":{\"name\":\"groupName\"}," +
                "\"uuid\":\"6e394949-9988-4459-85e9-feda224ac321\"" +
                "}");
    }

    @Test
    public void testJsonDeserialization() throws JsonProcessingException {
        final MappedOidcGroup mappedOidcGroup = new ObjectMapper().readValue("" +
                "{" +
                "\"id\":666," +
                "\"group\":{\"name\":\"groupName\"}," +
                "\"team\":{\"name\":\"teamName\"}," +
                "\"uuid\":\"6e394949-9988-4459-85e9-feda224ac321\"" +
                "}", MappedOidcGroup.class);

        assertThat(mappedOidcGroup.getId()).isZero();
        assertThat(mappedOidcGroup.getGroup()).isNotNull();
        assertThat(mappedOidcGroup.getTeam()).isNull();
        assertThat(mappedOidcGroup.getUuid()).isEqualTo(UUID.fromString("6e394949-9988-4459-85e9-feda224ac321"));
    }

}
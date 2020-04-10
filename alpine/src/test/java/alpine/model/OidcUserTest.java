package alpine.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcUserTest {

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        final Team team = new Team();
        team.setName("teamName");

        final Permission permission = new Permission();
        permission.setName("permissionName");

        final OidcUser oidcUser = new OidcUser();
        oidcUser.setId(666);
        oidcUser.setUsername("username");
        oidcUser.setSubjectIdentifier("subjectIdentifier");
        oidcUser.setEmail("username@mail.local");
        oidcUser.setTeams(Collections.singletonList(team));
        oidcUser.setPermissions(Collections.singletonList(permission));

        assertThat(new ObjectMapper().writeValueAsString(oidcUser)).isEqualTo("" +
                "{" +
                "\"username\":\"username\"," +
                "\"subjectIdentifier\":\"subjectIdentifier\"," +
                "\"email\":\"username@mail.local\"," +
                "\"teams\":[{\"name\":\"teamName\"}]," +
                "\"permissions\":[{\"name\":\"permissionName\"}]" +
                "}");
    }

    @Test
    public void testJsonDeserialization() throws JsonProcessingException {
        final OidcUser oidcUser = new ObjectMapper().readValue("" +
                "{" +
                "\"id\":666," +
                "\"username\":\"username\"," +
                "\"subjectIdentifier\":\"subjectIdentifier\"," +
                "\"email\":\"username@mail.local\"," +
                "\"teams\":[{\"name\":\"teamName\"}]," +
                "\"permissions\":[{\"name\":\"permissionName\"}]" +
                "}", OidcUser.class);

        assertThat(oidcUser.getId()).isZero();
        assertThat(oidcUser.getUsername()).isEqualTo("username");
        assertThat(oidcUser.getSubjectIdentifier()).isEqualTo("subjectIdentifier");
        assertThat(oidcUser.getEmail()).isEqualTo("username@mail.local");
        assertThat(oidcUser.getTeams()).hasSize(1);
        assertThat(oidcUser.getPermissions()).hasSize(1);
    }

}
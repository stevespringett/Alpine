package alpine.auth;

import java.util.List;

/**
 * @since 1.10.0
 */
class OidcProfile {

    private String subject;
    private String username;
    private List<String> teams;
    private String email;

    String getSubject() {
        return subject;
    }

    void setSubject(final String subject) {
        this.subject = subject;
    }

    String getUsername() {
        return username;
    }

    void setUsername(final String username) {
        this.username = username;
    }

    List<String> getTeams() {
        return teams;
    }

    void setTeams(final List<String> teams) {
        this.teams = teams;
    }

    String getEmail() {
        return email;
    }

    void setEmail(final String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "OidcProfile{" +
                "subjectIdentifier='" + subject + '\'' +
                ", username='" + username + '\'' +
                ", teams=" + teams +
                ", email='" + email + '\'' +
                '}';
    }

}

package alpine.auth;

import java.util.List;

/**
 * @since 1.10.0
 */
class OidcProfile {

    private String subject;
    private String username;
    private List<String> groups;
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

    List<String> getGroups() {
        return groups;
    }

    void setGroups(final List<String> groups) {
        this.groups = groups;
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
                "subject='" + subject + '\'' +
                ", username='" + username + '\'' +
                ", groups=" + groups +
                ", email='" + email + '\'' +
                '}';
    }

}

package alpine.auth;

/**
 * @since 1.8.0
 */
public enum IdentityProvider {

    LOCAL,

    LDAP,

    OPENID_CONNECT;

    /**
     * Returns an IdentityProvider that matches a given name.
     *
     * @param name Name of the IdentityProvider to get
     * @return The matching IdentityProvider, or null when no matching IdentityProvider was found
     */
    public static IdentityProvider forName(final String name) {
        for (final IdentityProvider identityProvider : values()) {
            if (identityProvider.name().equals(name)) {
                return identityProvider;
            }
        }
        return null;
    }

}

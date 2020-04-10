package alpine.auth;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentityProviderTest {

    @Test
    public void forNameShouldReturnMatchingIdentityProvider() {
        Arrays.stream(IdentityProvider.values()).forEach(identityProvider -> {
            assertThat(IdentityProvider.forName(identityProvider.name())).isEqualTo(identityProvider);
        });
    }

    @Test
    public void forNameShouldReturnNullWhenNoMatchingIdentityProviderExists() {
        assertThat(IdentityProvider.forName("doesNotExist")).isNull();
    }

}
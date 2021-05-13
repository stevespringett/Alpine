package alpine.auth;

import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;

/**
 * @since 1.10.0
 */
interface OidcProfileCreator {

    OidcProfile create(final ClaimsSet claimsSet);

}

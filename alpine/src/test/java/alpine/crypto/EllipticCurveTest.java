package alpine.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidParameterSpecException;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EllipticCurveTest {

    @BeforeClass
    public static void setUpClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void getParameterSpecShouldProvideParameterSpecsForAllCurves() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidParameterSpecException {
        for (final EllipticCurve curve : EllipticCurve.values()) {
            // An exception will be thrown in case no parameter spec can be provided
            curve.getParameterSpec();
        }
    }

    @Test
    public void forJwkNameShouldReturnMatchingCurve() {
        for (final EllipticCurve curve : EllipticCurve.values()) {
            assertThat(EllipticCurve.forJwkName(curve.getJwkName())).isEqualTo(curve);
        }
    }

    @Test
    public void forJwkNameShouldThrowExceptionWhenNoMatchingCurveCouldBeFound() {
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> EllipticCurve.forJwkName("unknown"));
    }

    @AfterClass
    public static void tearDownClass() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

}
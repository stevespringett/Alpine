package alpine.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

public class Base64EncodedBigIntegerDeserializerTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        final SimpleModule testModule = new SimpleModule();
        testModule.addDeserializer(BigInteger.class, new Base64EncodedBigIntegerDeserializer());

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(testModule);
    }

    @Test
    public void shouldDeserializeBase64EncodedBigInteger() throws JsonProcessingException {
        final BigInteger bigInteger = objectMapper.readValue("\"RpKjA_K7HvyEyaj-PSHTnW0oJt1D9qI-WOlDy_BRaN01K38PpgELT3djZC8xioSz\"", BigInteger.class);
        assertThat(bigInteger).isEqualTo("10862148045561831329173922231897049266569490074483719200916490864501209730647565488419450928996740556899663022294195");
    }

    @Test
    public void shouldThrowExceptionWhenNodeIsNotTextual() {
        assertThatIOException()
                .isThrownBy(() -> objectMapper.readValue("{}", BigInteger.class))
                .withMessageContaining("not a text value");
    }

    @Test
    public void shouldThrowExceptionWhenNodeIsNotBase64Encoded() {
        assertThatIOException()
                .isThrownBy(() -> objectMapper.readValue("\"!!!!\"", BigInteger.class))
                .withMessageContaining("not a Base64 encoded value");
    }

}
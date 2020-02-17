package alpine.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.math.BigInteger;

public class Base64EncodedBigIntegerDeserializer extends JsonDeserializer<BigInteger> {

    @Override
    public BigInteger deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException {
        final JsonNode node = jsonParser.readValueAsTree();

        if (!node.isTextual()) {
            throw new IOException("Node is not a text value");
        }

        final String encodedValue = node.asText();
        if (!Base64.isBase64(encodedValue)) {
            throw new IOException("Node is not a Base64 encoded value");
        }

        return new BigInteger(1, Base64.decodeBase64(encodedValue));
    }

}

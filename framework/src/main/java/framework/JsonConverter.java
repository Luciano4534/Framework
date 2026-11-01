package framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonConverter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonConverter() {
    }

    public static String toJson(Object value, boolean alreadyJson) throws JsonProcessingException {
        if (alreadyJson && value instanceof String json) {
            return json;
        }
        /*conversion en Json */
        return OBJECT_MAPPER.writeValueAsString(value);
    }
}

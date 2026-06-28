package com.tourya.api.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.tourya.api._utils.PayloadIdUtils;

import java.io.IOException;

/**
 * Normaliza ids del front: {@code ""} y {@code 0} se interpretan como null (registro nuevo).
 */
public class EmptyStringAsNullIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING && p.getText().trim().isEmpty()) {
            return null;
        }
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        int value = p.getValueAsInt();
        return PayloadIdUtils.isTransientId(value) ? null : value;
    }
}

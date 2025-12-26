package com.tourya.api.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tourya.api.models.TranslatedField;

import java.io.IOException;

/**
 * Custom Jackson deserializer for TranslatedField that accepts both:
 * - String values (converted to TranslatedField with Spanish only)
 * - Object values with { "es": "...", "en": "...", "pt": "..." }
 */
public class TranslatedFieldDeserializer extends JsonDeserializer<TranslatedField> {

    @Override
    public TranslatedField deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node == null || node.isNull()) {
            return null;
        }
        
        // Si es un String, convertir a TranslatedField con el texto en español
        if (node.isTextual()) {
            return TranslatedField.ofSpanish(node.asText());
        }
        
        // Si es un objeto, deserializar normalmente
        if (node.isObject()) {
            String es = node.has("es") ? node.get("es").asText() : null;
            String en = node.has("en") ? node.get("en").asText(null) : null;
            String pt = node.has("pt") ? node.get("pt").asText(null) : null;
            
            if (es == null) {
                throw new IllegalArgumentException("El campo 'es' es obligatorio en TranslatedField");
            }
            
            return new TranslatedField(es, en != null ? en : "", pt != null ? pt : "");
        }
        
        throw new IllegalArgumentException("TranslatedField debe ser String o un objeto con campos 'es', 'en', 'pt'");
    }
}


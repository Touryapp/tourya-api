package com.tourya.api.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.TranslatedField;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA AttributeConverter for TranslatedField to JSONB conversion.
 * Handles serialization and deserialization between TranslatedField objects
 * and PostgreSQL JSONB column type.
 */
@Slf4j
@Converter(autoApply = false)
public class TranslatedFieldConverter implements AttributeConverter<TranslatedField, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert TranslatedField to JSON string for database storage.
     *
     * @param field TranslatedField object
     * @return JSON string representation
     */
    @Override
    public String convertToDatabaseColumn(TranslatedField field) {
        if (field == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(field);
        } catch (JsonProcessingException e) {
            log.error("Error converting TranslatedField to JSON: {}", e.getMessage());
            throw new IllegalArgumentException("Error converting TranslatedField to JSON", e);
        }
    }

    /**
     * Convert JSON string from database to TranslatedField object.
     *
     * @param dbData JSON string from database
     * @return TranslatedField object
     */
    @Override
    public TranslatedField convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(dbData, TranslatedField.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to TranslatedField: {}", e.getMessage());
            throw new IllegalArgumentException("Error parsing JSON to TranslatedField", e);
        }
    }
}

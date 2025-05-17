package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProveedorStatusEnumConverter implements AttributeConverter<ProveedorStatusEnum, String> {
    public String convertToDatabaseColumn(ProveedorStatusEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public ProveedorStatusEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return ProveedorStatusEnum.of(value);
    }
}

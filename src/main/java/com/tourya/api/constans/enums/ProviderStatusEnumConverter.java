package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProviderStatusEnumConverter implements AttributeConverter<ProviderStatusEnum, String> {
    public String convertToDatabaseColumn(ProviderStatusEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public ProviderStatusEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return ProviderStatusEnum.of(value);
    }
}

package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProviderServiceTypeEnumConverter implements AttributeConverter<ProviderServiceTypeEnum, String>

{
    public String convertToDatabaseColumn(ProviderServiceTypeEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public ProviderServiceTypeEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return ProviderServiceTypeEnum.of(value);
    }
}

package com.tourya.api.constans.enums;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CancellationPolicyTypeEnumConverter implements AttributeConverter<CancellationPolicyTypeEnum, String> {
    public String convertToDatabaseColumn(CancellationPolicyTypeEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public CancellationPolicyTypeEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return CancellationPolicyTypeEnum.of(value);
    }
}

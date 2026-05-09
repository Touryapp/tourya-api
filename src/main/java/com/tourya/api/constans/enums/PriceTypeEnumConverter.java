package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PriceTypeEnumConverter implements AttributeConverter<PriceTypeEnum, String> {
    public String convertToDatabaseColumn(PriceTypeEnum value) {
        if (value == null) {
            return null;
        }

        return value.getValue();
    }

    public PriceTypeEnum convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return PriceTypeEnum.of(value);
    }
}

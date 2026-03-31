package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TourDurationEnumConverter implements AttributeConverter<TourDurationEnum, String> {
    @Override
    public String convertToDatabaseColumn(TourDurationEnum value) {
        return value == null ? null : value.getValue();
    }

    @Override
    public TourDurationEnum convertToEntityAttribute(String value) {
        return value == null ? null : TourDurationEnum.of(value);
    }
}


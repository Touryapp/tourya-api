package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TourStatusEnumConverter implements AttributeConverter<TourStatusEnum, String> {
    public String convertToDatabaseColumn(TourStatusEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public TourStatusEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return TourStatusEnum.of(value);
    }
}

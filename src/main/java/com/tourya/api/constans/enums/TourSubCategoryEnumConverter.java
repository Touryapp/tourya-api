package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TourSubCategoryEnumConverter implements AttributeConverter<TourSubCategoryEnum, String> {
    @Override
    public String convertToDatabaseColumn(TourSubCategoryEnum value) {
        return value == null ? null : value.getValue();
    }

    @Override
    public TourSubCategoryEnum convertToEntityAttribute(String value) {
        return value == null ? null : TourSubCategoryEnum.of(value);
    }
}


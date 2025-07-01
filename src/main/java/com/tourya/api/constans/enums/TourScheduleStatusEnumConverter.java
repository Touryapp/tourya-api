package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TourScheduleStatusEnumConverter implements AttributeConverter<TourScheduleStatusEnum, String> {
    public String convertToDatabaseColumn(TourScheduleStatusEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public TourScheduleStatusEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return TourScheduleStatusEnum.of(value);
    }
}

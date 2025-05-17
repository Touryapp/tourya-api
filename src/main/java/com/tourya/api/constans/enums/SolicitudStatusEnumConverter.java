package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SolicitudStatusEnumConverter implements AttributeConverter<SolicitudStatusEnum, String> {
    public String convertToDatabaseColumn(SolicitudStatusEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public SolicitudStatusEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return SolicitudStatusEnum.of(value);
    }
}

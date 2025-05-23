package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AddressTypeEnumConverter implements AttributeConverter<AddressTypeEnum, String> {
    public String convertToDatabaseColumn(AddressTypeEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public AddressTypeEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return AddressTypeEnum.of(value);
    }
}

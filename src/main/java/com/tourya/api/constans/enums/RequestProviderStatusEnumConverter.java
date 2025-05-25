package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RequestProviderStatusEnumConverter implements AttributeConverter<RequestProviderStatusEnum, String> {
    public String convertToDatabaseColumn(RequestProviderStatusEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public RequestProviderStatusEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return RequestProviderStatusEnum.of(value);
    }
}

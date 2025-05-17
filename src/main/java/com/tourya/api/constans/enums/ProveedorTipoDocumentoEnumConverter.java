package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProveedorTipoDocumentoEnumConverter implements AttributeConverter<ProveedorTipoDocumentoEnum, String>

    {
        public String convertToDatabaseColumn(ProveedorTipoDocumentoEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

        public ProveedorTipoDocumentoEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return ProveedorTipoDocumentoEnum.of(value);
    }
}

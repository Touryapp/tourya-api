package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProveedorTipoServicioEnumConverte implements AttributeConverter<ProveedorTipoServicioEnum, String>

{
    public String convertToDatabaseColumn(ProveedorTipoServicioEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public ProveedorTipoServicioEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return ProveedorTipoServicioEnum.of(value);
    }
}

package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProveedorTipoServicioEnum {
    TOUR("Tour"),
    TRANSPORTE("Trasnporte"),
    ALIMENTACION("Alimentacion"),
    ALOJAMIENTO("Alojamiento");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProveedorTipoServicioEnum of(String value) {
        for (ProveedorTipoServicioEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("ProveedorTipoServicioEnum: unknown value: " + value);
    }
}

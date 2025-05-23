package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProveedorStatusEnum {
    POTENCIAL("potencial"),
    ACTIVO("activo"),
    INACTIVO("inactivo"),
    BLOQUEADO("bloqueado");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProveedorStatusEnum of(String value) {
        for (ProveedorStatusEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("ProveedorStatusEnum: unknown value: " + value);
    }
}

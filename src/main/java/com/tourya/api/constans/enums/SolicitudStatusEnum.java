package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public enum SolicitudStatusEnum {
    PENDIENTE("pendiente"),
    EN_PROCESO("en_proceso"),
    APROBADA("aprobada"),
    RECHAZADA("rechazada"),
    COMPLETADA("completada"),
    CANCELADA("cancelada");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SolicitudStatusEnum of(String value) {
        for (SolicitudStatusEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("ProveedorStatusEnum: unknown value: " + value);
    }

}

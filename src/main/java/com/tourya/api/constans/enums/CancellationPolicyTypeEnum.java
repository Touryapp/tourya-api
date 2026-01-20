package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CancellationPolicyTypeEnum {
    FLEXIBLE("Flexible"), // hasta 24 horas antes
    STANDARD("Standard"), // hasta 48 horas antes
    MODERATE("Moderate"), // hasta 4 días antes
    STRICT("Strict"); // hasta 7 días antes

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CancellationPolicyTypeEnum of(String value) {
        for (CancellationPolicyTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("CancellationPolicyTypeEnum: unknown value: " + value);
    }
}

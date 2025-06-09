package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CancellationPolicyTypeEnum {
    FLEXIBLE("Flexible"),
    MODERATE("Moderate"),
    NON_REFUNDABLE("Non-refundable");

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

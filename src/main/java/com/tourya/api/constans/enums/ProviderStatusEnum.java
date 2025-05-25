package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProviderStatusEnum {
    POTENTIAL("Potential"),
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    BLOCKED("Blocked");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProviderStatusEnum of(String value) {
        for (ProviderStatusEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("ProviderStatusEnum: unknown value: " + value);
    }
}

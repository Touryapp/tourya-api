package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProviderDocumentTypeEnum {
    NIT("NIT"),
    RNT("RNT");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProviderDocumentTypeEnum of(String value) {
        for (ProviderDocumentTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("ProviderDocumentTypeEnum: unknown value: " + value);
    }

}

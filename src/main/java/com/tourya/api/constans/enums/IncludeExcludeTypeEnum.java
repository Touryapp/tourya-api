package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public enum IncludeExcludeTypeEnum {
    INCLUDE("include"),
    EXCLUDE("exclude");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static IncludeExcludeTypeEnum of(String value) {
        for (IncludeExcludeTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("IncludeExcludeTypeEnum: unknown value: " + value);
    }
}
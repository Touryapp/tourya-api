package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TourTimeOfDayEnum {
    MORNING("manana"),
    AFTERNOON("tarde"),
    NIGHT("noche");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TourTimeOfDayEnum of(String value) {
        for (TourTimeOfDayEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new UnknownEnumValueException("TourTimeOfDayEnum: unknown value: " + value);
    }
}


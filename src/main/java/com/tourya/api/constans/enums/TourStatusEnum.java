package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TourStatusEnum {
    CREATED("created"),
    SUBMITTED("submitted"),
    RETURNED("returned"),
    ACCEPTED("accepted"),
    CANCELLED("cancelled");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TourStatusEnum of(String value) {
        for (TourStatusEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("TourStatusEnum: unknown value: " + value);
    }
}

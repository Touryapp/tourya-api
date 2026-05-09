package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AddressTypeEnum {
    MEETING_POINT("Meeting Point"),
    END_POINT("End Point"),
    PICK_UP_POINT("Pick-up Point");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AddressTypeEnum of(String value) {
        for (AddressTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("AddressTypeEnum: unknown value: " + value);
    }
}

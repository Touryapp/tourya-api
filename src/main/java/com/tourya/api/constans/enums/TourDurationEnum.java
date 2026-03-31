package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TourDurationEnum {
    H1_2("1_a_2_horas"),
    H2_4("2_a_4_horas"),
    H4_6("4_a_6_horas"),
    D1("hasta_1_dia"),
    D3("hasta_3_dias"),
    D5("hasta_5_dias");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TourDurationEnum of(String value) {
        for (TourDurationEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new UnknownEnumValueException("TourDurationEnum: unknown value: " + value);
    }
}


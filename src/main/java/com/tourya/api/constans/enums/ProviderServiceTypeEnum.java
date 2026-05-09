package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProviderServiceTypeEnum {
    TOUR("Tour"),
    TRANSPORT("Transport"),
    MEALS_FOOD_BEVERAGE("Meals or Food & Beverage (F&B)"),
    ACCOMMODATION_LODGING("Accommodation or Lodging");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProviderServiceTypeEnum of(String value) {
        for (ProviderServiceTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("ProviderServiceTypeEnum: unknown value: " + value);
    }
}

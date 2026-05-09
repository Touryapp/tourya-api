package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public enum RequestProviderStatusEnum {
    CREATED("Created"),
    SUBMITTED("Submitted"),
    PRE_APPROVED("Pre-Approved"),
    DOCUMENTS_SENT("Documents Sent"),
    APPROVED("Approved"),
    INCOMPLETE_INFORMATION ("Incomplete Information"),
    CANCELLED("Cancelled");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RequestProviderStatusEnum of(String value) {
        for (RequestProviderStatusEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("RequestProviderStatusEnum: unknown value: " + value);
    }

}

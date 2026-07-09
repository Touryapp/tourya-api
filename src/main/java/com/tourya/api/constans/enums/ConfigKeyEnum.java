package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ConfigKeyEnum {
    CANCELLATION_POLICY("CANCELLATION_POLICY"),
    HOLD_MINUTES("HOLD_MINUTES"),
    PAYOUT_BUFFER_DAYS("PAYOUT_BUFFER_DAYS"),
    CREDIT_EXPIRATION_MONTHS("CREDIT_EXPIRATION_MONTHS"),
    GALLERY_MAX_SIZE_MB("GALLERY_MAX_SIZE_MB"),
    GALLERY_MIN_WIDTH_PX("GALLERY_MIN_WIDTH_PX"),
    GALLERY_MAX_IMAGES_PER_TOUR("GALLERY_MAX_IMAGES_PER_TOUR"),
    KYB_REQUIRE_MANDATORY_DOCS("KYB_REQUIRE_MANDATORY_DOCS"),
    AUTH_RATE_LIMIT_ENABLED("AUTH_RATE_LIMIT_ENABLED"),
    AUTH_RATE_LIMIT_PER_MINUTE("AUTH_RATE_LIMIT_PER_MINUTE");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ConfigKeyEnum of(String value) {
        for (ConfigKeyEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("ConfigKeyEnum: unknown value: " + value);
    }
}


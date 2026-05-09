package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum que representa los tipos de métodos de pago disponibles.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
public enum PaymentMethodTypeEnum {
    CARD("CARD"),
    NEQUI("NEQUI"),
    BANCOLOMBIA_TRANSFER("BANCOLOMBIA_TRANSFER"),
    PSE("PSE"),
    CASH("CASH"),
    BANK_TRANSFER("BANK_TRANSFER");

    private final String value;

    PaymentMethodTypeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PaymentMethodTypeEnum fromValue(String value) {
        for (PaymentMethodTypeEnum type : PaymentMethodTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PaymentMethodTypeEnum value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}

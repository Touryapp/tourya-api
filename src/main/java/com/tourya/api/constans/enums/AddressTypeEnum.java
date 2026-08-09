package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AddressTypeEnum {
    MEETING_POINT("Meeting Point"),
    END_POINT("End Point"),
    PICK_UP_POINT("Pick-up Point"),
    // TC-017 (#220): recogida en el hotel del cliente. Sin ubicacion fisica del tour.
    HOTEL_PICKUP("Hotel Pickup");

    private String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AddressTypeEnum of(String value) {
        for (AddressTypeEnum e : values()) {
            // TC-017 (#220): acepta tanto el display value ("Hotel Pickup") como la key ("HOTEL_PICKUP")
            // para tolerar frontends que envien cualquiera de los dos formatos.
            if (e.value.equalsIgnoreCase(value) || e.name().equalsIgnoreCase(value)) {
                return e;
            }
        }

        throw new UnknownEnumValueException("AddressTypeEnum: unknown value: " + value);
    }
}

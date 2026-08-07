package com.tourya.api.models.request;


import com.tourya.api.constans.enums.AddressTypeEnum;
import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourAddressRequest {
    private Integer Id;
    // TC-017 (#220): country/state/city opcionales para permitir addressType=HOTEL_PICKUP
    // (recogida en el hotel del cliente, sin ubicacion fisica del tour). El frontend
    // valida la exclusividad segun el tipo elegido.
    private Integer countryId;

    private Integer stateId;

    private Integer cityId;

    private Double latitude = 0.0;

    private Double longitude = 0.0;

    private String address;

    @Valid
    private TranslatedField location;

    @NotNull(message = "addressType is mandatory")
    private AddressTypeEnum addressType;

}

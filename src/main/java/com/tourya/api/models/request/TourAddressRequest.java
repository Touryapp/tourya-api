package com.tourya.api.models.request;


import com.tourya.api.constans.enums.AddressTypeEnum;
import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourAddressRequest {
    private Integer Id;
    @NotNull(message = "countryId is mandatory")
    private Integer countryId;

    @NotNull(message = "stateId is mandatory")
    private Integer stateId;

    @NotNull(message = "cityId is mandatory")
    private Integer cityId;

    private Double latitude = 0.0;

    private Double longitude = 0.0;

    private String address;

    @Valid
    private TranslatedField location;

    @NotNull(message = "addressType is mandatory")
    private AddressTypeEnum addressType;

}

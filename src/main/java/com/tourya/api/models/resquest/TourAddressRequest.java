package com.tourya.api.models.resquest;


import com.tourya.api.constans.enums.AddressTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourAddressRequest {

    @NotNull(message = "countryId is mandatory")
    private Integer countryId;

    @NotNull(message = "stateId is mandatory")
    private Integer stateId;

    @NotNull(message = "cityId is mandatory")
    private Integer cityId;

    private Double latitude = 0.0;

    private Double longitude = 0.0;

    private String address;

    private AddressTypeEnum addressType;

}

package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.AddressTypeEnum;
import lombok.Data;

@Data
public class TourAddressResponse {
    private Integer id;
    private String address;
    private String location;
    private AddressTypeEnum addressType;
    private Integer countryId;
    private Integer stateId;
    private Integer cityId;
    private Double latitude;
    private Double longitude;
    //private TourResponse tour;
}

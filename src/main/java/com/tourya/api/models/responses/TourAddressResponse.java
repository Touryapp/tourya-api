package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.AddressTypeEnum;
import lombok.Data;

@Data
public class TourAddressResponse {
    private Integer id;
    private String address;
    private AddressTypeEnum addressType;
    private Double latitude;
    private Double longitude;
    private TourResponse tour;
}

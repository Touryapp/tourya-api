package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class TourDetailsResponse {
    private TourResponse tourResponse;
    private TourAddressResponse tourAddressResponse;
}

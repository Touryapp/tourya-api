package com.tourya.api.models.responses;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TourAddressListResponse {
    private List<TourAddressResponse> tourAddressListResponse = new ArrayList<>();
}

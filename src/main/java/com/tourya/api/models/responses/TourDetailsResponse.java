package com.tourya.api.models.responses;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TourDetailsResponse {
    private Integer id;
    private String name;
    private String description;
    private String duration;
    private Integer maxPeople;
    private Integer highlight;
    private Integer tourCategoryId;
    private List<TourAddressResponse> locations = new ArrayList<>();
}

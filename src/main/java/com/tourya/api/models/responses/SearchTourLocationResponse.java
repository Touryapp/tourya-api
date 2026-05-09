package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class SearchTourLocationResponse {
    private Integer countryId;
    private String countryName;
    private Integer stateId;
    private String stateName;
    private Integer cityId;
    private String cityName;
}
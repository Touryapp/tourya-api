package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class CityResponse {
    private Integer id;
    private String name;
    private StateResponse state;
}

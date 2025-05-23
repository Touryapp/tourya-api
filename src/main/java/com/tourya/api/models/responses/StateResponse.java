package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class StateResponse {
    private Integer id;
    private String name;
    private CountryResponse country;
}

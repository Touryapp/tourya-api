package com.tourya.api.models.responses;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourLocationInSearchDto {
    private String address;
    private String location;
    private String cityName;
    private String stateName;
    private String countryName;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

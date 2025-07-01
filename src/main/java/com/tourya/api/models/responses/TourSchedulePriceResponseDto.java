package com.tourya.api.models.responses;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourSchedulePriceResponseDto {
    private Integer id;
    private String ageType;
    private Integer minAge;
    private Integer maxAge;
    private BigDecimal price;
}

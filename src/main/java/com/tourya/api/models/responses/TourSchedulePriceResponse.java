package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.AgePriceType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourSchedulePriceResponse {
    private Integer id;
    private AgePriceType ageType;
    private Integer minAge;
    private Integer maxAge;
    private BigDecimal price;
}

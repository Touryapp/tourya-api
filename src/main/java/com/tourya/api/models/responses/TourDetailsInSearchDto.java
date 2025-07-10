package com.tourya.api.models.responses;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourDetailsInSearchDto {
    private Integer tourId;
    private String tourName;
    private String description;
    private Integer minAge;
    private BigDecimal rating;
    private String categoryName; // Nombre de la categoría
    private Integer providerId;
}

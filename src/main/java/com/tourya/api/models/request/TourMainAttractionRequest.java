package com.tourya.api.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class TourMainAttractionRequest {
    @NotEmpty(message = "description is mandatory")
    @NotNull(message = "description is mandatory")
    private String description;
}
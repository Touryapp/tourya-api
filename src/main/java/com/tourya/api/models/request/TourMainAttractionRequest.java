package com.tourya.api.models.request;

import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class TourMainAttractionRequest {
    private Integer id;
    @Valid
    @NotNull(message = "description is mandatory")
    private TranslatedField description;
}
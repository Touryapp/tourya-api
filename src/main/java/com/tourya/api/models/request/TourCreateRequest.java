package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TourCreateRequest {

    @NotEmpty(message = "name is mandatory")
    @NotNull(message = "name is mandatory")
    private String name;

    @NotEmpty(message = "description is mandatory")
    @NotNull(message = "description is mandatory")
    private String description;

    @NotNull(message = "tourCategoryId is mandatory")
    private Integer tourCategoryId;

    private String duration;

    private Integer maxPeople = 0; // Valor por defecto 0

    private Integer highlight = 0; // Valor por defecto 0

    @Valid
    @NotNull(message = "locations is mandatory")
    private List<TourAddressRequest> locations;
}

package com.tourya.api.models.resquest;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourRequest {

    @NotEmpty(message = "Name is mandatory")
    @NotNull(message = "Name is mandatory")
    private String name;

    @NotEmpty(message = "Description is mandatory")
    @NotNull(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Tour Category ID is mandatory")
    private Integer tourCategoryId;

    private String duration;

    private Integer maxPeople = 0; // Valor por defecto 0

    private Integer highlight = 0; // Valor por defecto 0

}

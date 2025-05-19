package com.tourya.api.models.resquest;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourCategoryRequest {
    @NotEmpty(message = "Name is mandatory")
    @NotNull(message = "Name is mandatory")
    private String name;

    @NotEmpty(message = "Description is mandatory")
    @NotNull(message = "Description is mandatory")
    private String description;
}

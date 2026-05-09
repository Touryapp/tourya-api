package com.tourya.api.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourCancelCategoryRequest {
    @NotEmpty(message = "name is mandatory")
    @NotNull(message = "name is mandatory")
    private String name;
    @NotEmpty(message = "description is mandatory")
    @NotNull(message = "description is mandatory")
    private String description;
}

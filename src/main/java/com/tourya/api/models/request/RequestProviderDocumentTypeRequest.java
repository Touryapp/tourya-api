package com.tourya.api.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestProviderDocumentTypeRequest {

    @NotBlank(message = "Name is mandatory")
    private String name;

    private String description;

    @NotNull(message = "Mandatory flag is required")
    private Boolean mandatory;
}

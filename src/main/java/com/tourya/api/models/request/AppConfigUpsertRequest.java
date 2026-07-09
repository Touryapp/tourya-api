package com.tourya.api.models.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AppConfigUpsertRequest {

    @NotNull(message = "value is mandatory")
    private Map<String, Object> value;

    private String description;
}

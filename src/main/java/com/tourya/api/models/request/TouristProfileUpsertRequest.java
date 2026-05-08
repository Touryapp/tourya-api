package com.tourya.api.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class TouristProfileUpsertRequest {

    @Schema(example = "Donna")
    private String firstName;

    @Schema(example = "González")
    private String lastName;

    @Schema(description = "Número de documento de identidad", example = "1020304050")
    private String documentNumber;

    @Schema(example = "+57 3001234567")
    private String phone;

    @Email
    @Schema(example = "donna@example.com")
    private String email;

    @Schema(example = "Cartagena")
    private String city;

    @Schema(example = "Bolívar")
    private String state;

    @Schema(example = "Colombia")
    private String country;
}


package com.tourya.api.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetProviderOperatorPasswordRequest {

    @NotBlank
    @Size(min = 8, message = "temporaryPassword should be 8 characters long minimum")
    private String temporaryPassword;
}

package com.tourya.api.models.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotEmpty(message = "currentPassword is mandatory")
    @NotNull(message = "currentPassword is mandatory")
    @Size(min = 8, message = "currentPassword should be 8 characters long minimum")
    private String currentPassword;

    @NotEmpty(message = "newPassword is mandatory")
    @NotNull(message = "newPassword is mandatory")
    @Size(min = 8, message = "newPassword should be 8 characters long minimum")
    private String newPassword;

    @NotEmpty(message = "confirmationPassword is mandatory")
    @NotNull(message = "confirmationPassword is mandatory")
    @Size(min = 8, message = "confirmationPassword should be 8 characters long minimum")
    private String confirmationPassword;
}

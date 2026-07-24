package com.tourya.api.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * FE-15d: request para resetear la contraseña de un usuario
 * {@code BACKOFFICE_OPERATION}. El usuario deberá cambiarla en el próximo login.
 */
@Data
public class ResetBackofficeUserPasswordRequest {

    @NotBlank
    @Size(min = 8, message = "temporaryPassword should be 8 characters long minimum")
    private String temporaryPassword;
}

package com.tourya.api.models.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * FE-15d: request para actualizar datos personales de un usuario
 * {@code BACKOFFICE_OPERATION}. El cambio de contraseña va por endpoint aparte.
 */
@Data
public class UpdateBackofficeUserRequest {

    private String firstname;

    private String lastname;

    @Size(max = 20)
    private String phone;
}

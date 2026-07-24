package com.tourya.api.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * FE-15d: request para crear un usuario con rol {@code BACKOFFICE_OPERATION}.
 * Solo lo puede llamar un usuario ADMIN. El usuario creado debe cambiar la
 * contraseña temporal en el primer acceso ({@code mustChangePassword = true}).
 */
@Data
public class CreateBackofficeUserRequest {

    @NotBlank
    private String firstname;

    private String lastname;

    @NotBlank
    @Email
    private String email;

    /** Opcional. Sirve para contacto interno. */
    @Size(max = 20)
    private String phone;

    /** Contraseña temporal que el ADMIN comunica al operador; debe cambiarla en el primer acceso. */
    @NotBlank
    @Size(min = 8, message = "temporaryPassword should be 8 characters long minimum")
    private String temporaryPassword;
}

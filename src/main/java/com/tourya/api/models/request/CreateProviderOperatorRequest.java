package com.tourya.api.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateProviderOperatorRequest {

    @NotBlank
    private String firstname;

    private String lastname;

    @NotBlank
    @Email
    private String email;

    /** BE-22d: telefono de contacto del operador. Opcional. Usado como contacto principal del tour (RN-026). */
    @Size(max = 20)
    private String phone;

    /** Contraseña temporal que el proveedor comunica al operador; debe cambiarla en el primer acceso. */
    @NotBlank
    @Size(min = 8, message = "temporaryPassword should be 8 characters long minimum")
    private String temporaryPassword;

    /** Tours asignados al operador. */
    @NotEmpty
    private List<Integer> tourIds;

    /** Tour donde este operador es el responsable principal. */
    private Integer principalTourId;
}

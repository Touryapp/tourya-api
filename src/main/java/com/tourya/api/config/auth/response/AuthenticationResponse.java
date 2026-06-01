package com.tourya.api.config.auth.response;

import com.tourya.api.models.Role;
import com.tourya.api.models.responses.MetaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@Schema(description = "Respuesta de autenticación (login / social)")
public class AuthenticationResponse {
    private MetaResponse meta = new MetaResponse();
    private String fullName;
    private String email;
    private List<Role> roleList;
    private String token;
    @Schema(description = "Si es true, el cliente debe llamar PATCH /users para cambiar contraseña antes de continuar.")
    private Boolean mustChangePassword;
}

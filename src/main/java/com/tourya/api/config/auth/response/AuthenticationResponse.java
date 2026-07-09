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

    @Schema(description = "Access token JWT. Alias legacy: mismo valor que accessToken. Los nuevos clientes deben leer accessToken.")
    private String token;

    @Schema(description = "Access token JWT (usar este; token es alias legacy).")
    private String accessToken;

    @Schema(description = "Refresh token JWT. Usar POST /auth/refresh para rotar antes de que expire el access token.")
    private String refreshToken;

    @Schema(description = "Si es true, el cliente debe llamar PATCH /users para cambiar contraseña antes de continuar.")
    private Boolean mustChangePassword;
}

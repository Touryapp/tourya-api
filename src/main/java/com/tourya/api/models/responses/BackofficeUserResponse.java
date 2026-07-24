package com.tourya.api.models.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * FE-15d: DTO de respuesta para usuarios con rol {@code BACKOFFICE_OPERATION}.
 */
@Data
@Builder
public class BackofficeUserResponse {
    private Integer userId;
    private String email;
    private String fullName;
    private String phone;
    private Boolean accountEnabled;
    @Schema(description = "true hasta que el operador cambie la contraseña temporal")
    private Boolean mustChangePassword;
}

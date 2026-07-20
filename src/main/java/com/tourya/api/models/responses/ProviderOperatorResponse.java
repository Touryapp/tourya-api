package com.tourya.api.models.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ProviderOperatorResponse {
    private Integer providerUserId;
    private Integer userId;
    private String email;
    private String fullName;
    /** BE-22d: telefono del operador. Nullable hasta que lo carguen. */
    private String phone;
    private Boolean isPrimary;
    private Boolean accountEnabled;
    /** true hasta que el operador cambie la contraseña temporal (PATCH /users). */
    @Schema(description = "true hasta que el operador cambie la contraseña temporal")
    private Boolean mustChangePassword;
    @Builder.Default
    private List<ProviderOperatorTourResponse> tours = new ArrayList<>();
}

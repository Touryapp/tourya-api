package com.tourya.api.config.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * SEC-06: request de POST /auth/facebook. El frontend envia el
 * {@code accessToken} que obtuvo de Facebook JavaScript SDK; el backend valida
 * via Graph API {@code debug_token} y recupera el perfil.
 */
@Getter
@Setter
public class FacebookAuthRequest {
    @NotBlank(message = "accessToken is mandatory")
    private String accessToken;
}

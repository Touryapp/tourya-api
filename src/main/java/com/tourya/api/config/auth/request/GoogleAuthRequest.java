package com.tourya.api.config.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * SEC-06: request de POST /auth/google. El frontend envia el {@code idToken}
 * que obtuvo de Google Identity Services; el backend valida su firma contra
 * las claves publicas de Google y verifica que fue emitido para nuestro
 * client id (audience check).
 */
@Getter
@Setter
public class GoogleAuthRequest {
    @NotBlank(message = "idToken is mandatory")
    private String idToken;
}

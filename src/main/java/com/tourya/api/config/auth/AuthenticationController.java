package com.tourya.api.config.auth;

import com.tourya.api.config.auth.request.AuthenticationRequest;
import com.tourya.api.config.auth.request.RefreshTokenRequest;
import com.tourya.api.config.auth.request.SocialAuthRequest;
import com.tourya.api.config.auth.request.RegistrationRequest;
import com.tourya.api.config.auth.response.AuthenticationResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthenticationController {
    private final AuthenticationService service;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Registro de usuario", description = "Envía email de activación con ACTIVATION_URL.")
    public ResponseEntity<?> register(
            @RequestBody @Valid RegistrationRequest request
    ) throws MessagingException {
        service.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Login", description = "Devuelve JWT. Si mustChangePassword=true (p. ej. operador con clave temporal), usar PATCH /users.")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @GetMapping("/activate-account")
    @Operation(summary = "Activar cuenta", description = "Token de 6 dígitos enviado por email al registrarse.")
    public void confirm(
            @RequestParam String token
    ) throws MessagingException {
        service.activateAccount(token);
    }

    @PostMapping("/social-auth")
    @Operation(summary = "Login social (Google)")
    public ResponseEntity<AuthenticationResponse> authenticateWithSocial(
            @RequestBody @Valid SocialAuthRequest request
    ){
        return ResponseEntity.ok(service.authenticateWithSocial(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotar tokens", description = "Recibe un refresh token valido, revoca ese (rotated) y emite un nuevo par access + refresh (mismo family_id). Si detecta reuso, revoca toda la familia y devuelve 401.")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestBody @Valid RefreshTokenRequest request
    ){
        return ResponseEntity.ok(service.refreshTokens(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion", description = "Revoca toda la familia del refresh token proporcionado. Idempotente: si el token es invalido, tambien retorna 204.")
    public ResponseEntity<Void> logout(
            @RequestBody @Valid RefreshTokenRequest request
    ){
        service.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @GetMapping("/sendEmailTest")
    public void sendEmailTest(
    ) throws MessagingException {
        service.sendEmailTest();
    }


}

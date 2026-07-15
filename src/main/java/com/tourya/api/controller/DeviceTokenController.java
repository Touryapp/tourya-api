package com.tourya.api.controller;

import com.tourya.api.models.request.RegisterDeviceTokenRequest;
import com.tourya.api.models.request.UnregisterDeviceTokenRequest;
import com.tourya.api.services.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MO-40 Fase A: endpoints para registrar/quitar FCM tokens. Ambos requieren
 * JWT (endpoint no publico). El cliente mobile llama {@code POST} al login y
 * cuando el sistema le entrega un token nuevo (rotacion), y {@code DELETE} al
 * logout.
 */
@RestController
@RequestMapping("/users/device-token")
@RequiredArgsConstructor
@Tag(name = "Device Tokens (push)")
public class DeviceTokenController {

    private final DeviceTokenService service;

    @PostMapping
    @Operation(summary = "Registrar device token (FCM)",
            description = "Registra o refresca el token del dispositivo del usuario logueado. Idempotente por token.")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterDeviceTokenRequest request,
            Authentication auth) {
        service.register(request.getToken(), request.getPlatform(), auth);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping
    @Operation(summary = "Quitar device token", description = "Se llama al logout para dejar de recibir push en ese dispositivo.")
    public ResponseEntity<Void> unregister(
            @Valid @RequestBody UnregisterDeviceTokenRequest request,
            Authentication auth) {
        service.unregister(request.getToken(), auth);
        return ResponseEntity.noContent().build();
    }
}

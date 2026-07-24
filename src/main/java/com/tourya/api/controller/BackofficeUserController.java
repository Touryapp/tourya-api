package com.tourya.api.controller;

import com.tourya.api.models.request.CreateBackofficeUserRequest;
import com.tourya.api.models.request.ResetBackofficeUserPasswordRequest;
import com.tourya.api.models.request.UpdateBackofficeUserRequest;
import com.tourya.api.models.responses.BackofficeUserResponse;
import com.tourya.api.services.BackofficeUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * FE-15d: gestión de usuarios con rol {@code BACKOFFICE_OPERATION}. Solo ADMIN.
 */
@RestController
@RequestMapping("admin/backoffice-users")
@RequiredArgsConstructor
@Tag(name = "Backoffice Users")
public class BackofficeUserController {

    private final BackofficeUserService backofficeUserService;

    @GetMapping
    @Operation(summary = "Listar usuarios BACKOFFICE_OPERATION")
    public ResponseEntity<List<BackofficeUserResponse>> list(Authentication connectedUser) {
        return ResponseEntity.ok(backofficeUserService.list(connectedUser));
    }

    @PostMapping
    @Operation(summary = "Crear usuario BACKOFFICE_OPERATION",
            description = "El ADMIN define una contraseña temporal. El usuario debe cambiarla en el primer login.")
    public ResponseEntity<BackofficeUserResponse> create(
            @Valid @RequestBody CreateBackofficeUserRequest request,
            Authentication connectedUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(backofficeUserService.create(request, connectedUser));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Actualizar datos personales del usuario BACKOFFICE_OPERATION")
    public ResponseEntity<BackofficeUserResponse> update(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateBackofficeUserRequest request,
            Authentication connectedUser) {
        return ResponseEntity.ok(backofficeUserService.update(userId, request, connectedUser));
    }

    @PutMapping("/{userId}/reset-password")
    @Operation(summary = "Resetear contraseña temporal del usuario BACKOFFICE_OPERATION")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Integer userId,
            @Valid @RequestBody ResetBackofficeUserPasswordRequest request,
            Authentication connectedUser) {
        backofficeUserService.resetTemporaryPassword(userId, request, connectedUser);
        return ResponseEntity.ok(Map.of("message", "Temporary password updated"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Deshabilitar usuario BACKOFFICE_OPERATION (no lo elimina, lo desactiva)")
    public ResponseEntity<Map<String, String>> disable(
            @PathVariable Integer userId,
            Authentication connectedUser) {
        backofficeUserService.toggleEnabled(userId, false, connectedUser);
        return ResponseEntity.ok(Map.of("message", "User disabled"));
    }

    @PutMapping("/{userId}/enable")
    @Operation(summary = "Reactivar usuario BACKOFFICE_OPERATION")
    public ResponseEntity<Map<String, String>> enable(
            @PathVariable Integer userId,
            Authentication connectedUser) {
        backofficeUserService.toggleEnabled(userId, true, connectedUser);
        return ResponseEntity.ok(Map.of("message", "User enabled"));
    }
}

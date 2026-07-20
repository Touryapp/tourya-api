package com.tourya.api.controller;

import com.tourya.api.models.request.CreateProviderOperatorRequest;
import com.tourya.api.models.request.ResetProviderOperatorPasswordRequest;
import com.tourya.api.models.request.UpdateProviderOperatorRequest;
import com.tourya.api.models.responses.ProviderOperatorResponse;
import com.tourya.api.services.ProviderUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("provider/users")
@RequiredArgsConstructor
@Tag(name = "Provider Users")
public class ProviderUserController {

    private final ProviderUserService providerUserService;

    @GetMapping
    @Operation(summary = "Listar operadores del proveedor")
    public ResponseEntity<List<ProviderOperatorResponse>> listOperators(Authentication connectedUser) {
        return ResponseEntity.ok(providerUserService.listOperators(connectedUser));
    }

    @PostMapping
    @Operation(summary = "Crear operador",
            description = "El proveedor define una contraseña temporal. El operador inicia sesión y debe cambiarla (PATCH /users).")
    public ResponseEntity<ProviderOperatorResponse> createOperator(
            @Valid @RequestBody CreateProviderOperatorRequest request,
            Authentication connectedUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerUserService.createOperator(request, connectedUser));
    }

    @PutMapping("/{providerUserId}")
    @Operation(summary = "Actualizar operador", description = "Nombre, tours asignados y/o tour principal.")
    public ResponseEntity<ProviderOperatorResponse> updateOperator(
            @PathVariable("providerUserId") Integer providerUserId,
            @Valid @RequestBody UpdateProviderOperatorRequest request,
            Authentication connectedUser) {
        return ResponseEntity.ok(providerUserService.updateOperator(providerUserId, request, connectedUser));
    }

    @PutMapping("/{providerUserId}/principal-tour")
    @Operation(summary = "Cambiar tour principal del operador")
    public ResponseEntity<ProviderOperatorResponse> updatePrincipalTour(
            @PathVariable("providerUserId") Integer providerUserId,
            @RequestParam("tourId") Integer tourId,
            Authentication connectedUser) {
        return ResponseEntity.ok(
                providerUserService.updatePrincipalTour(providerUserId, tourId, connectedUser));
    }

    @GetMapping("/tour/{tourId}")
    @Operation(summary = "BE-22c: Listar operadores asignados a un tour del PROVIDER autenticado. Cada operador incluye flag isPrincipal.")
    public ResponseEntity<List<ProviderOperatorResponse>> listOperatorsByTour(
            @PathVariable Integer tourId,
            Authentication connectedUser) {
        return ResponseEntity.ok(providerUserService.listOperatorsByTour(tourId, connectedUser));
    }

    @PutMapping("/{providerUserId}/reset-password")
    @Operation(summary = "Restablecer contraseña temporal",
            description = "El proveedor asigna una nueva contraseña temporal; el operador deberá cambiarla en el próximo login.")
    public ResponseEntity<Map<String, String>> resetTemporaryPassword(
            @PathVariable("providerUserId") Integer providerUserId,
            @Valid @RequestBody ResetProviderOperatorPasswordRequest request,
            Authentication connectedUser) {
        providerUserService.resetTemporaryPassword(providerUserId, request, connectedUser);
        return ResponseEntity.ok(Map.of("message", "Temporary password updated"));
    }
}

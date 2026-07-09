package com.tourya.api.controller;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.models.AppConfig;
import com.tourya.api.models.request.AppConfigUpsertRequest;
import com.tourya.api.services.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para gestionar configuraciones del sistema.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Tag(name = "AppConfig", description = "API para gestionar configuraciones del sistema")
@SecurityRequirement(name = "bearerAuth")
public class AppConfigController {

    private final AppConfigService appConfigService;

    /**
     * Obtiene el valor de una configuración por su clave
     * 
     * @param configKey Clave de la configuración (ej: CANCELLATION_POLICY)
     * @return Valor de la configuración en formato JSON
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "Obtener configuración por clave", 
               description = "Obtiene el valor de una configuración del sistema por su clave")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuración encontrada"),
            @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    public ResponseEntity<Map<String, Object>> getConfig(
            @Parameter(description = "Clave de la configuración (ej: CANCELLATION_POLICY)")
            @PathVariable String configKey) {
        log.info("Getting config for key: {}", configKey);

        ConfigKeyEnum keyEnum;
        try {
            keyEnum = ConfigKeyEnum.of(configKey);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> configValue = appConfigService.getConfigValue(keyEnum);
        return ResponseEntity.ok(configValue);
    }

    /**
     * Upsert de una configuracion por clave. Solo ADMIN.
     * Body: { "value": {...}, "description": "..." } — description es opcional.
     */
    @PutMapping("/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear o actualizar configuracion",
            description = "Solo ADMIN. Upsert por clave. El cuerpo debe incluir 'value' (Map<String,Object>).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuracion guardada"),
            @ApiResponse(responseCode = "400", description = "Clave desconocida o body invalido"),
            @ApiResponse(responseCode = "403", description = "No autorizado (rol ADMIN requerido)")
    })
    public ResponseEntity<AppConfig> upsertConfig(
            @Parameter(description = "Clave de la configuracion (ej: HOLD_MINUTES)")
            @PathVariable String configKey,
            @Valid @RequestBody AppConfigUpsertRequest request) {
        log.info("Upserting config for key: {}", configKey);

        ConfigKeyEnum keyEnum;
        try {
            keyEnum = ConfigKeyEnum.of(configKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        AppConfig saved = appConfigService.upsertConfig(keyEnum, request.getValue(), request.getDescription());
        return ResponseEntity.ok(saved);
    }
}


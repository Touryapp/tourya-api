package com.tourya.api.controller;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.services.AppConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
}


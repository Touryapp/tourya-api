package com.tourya.api.controller;

import com.tourya.api.models.responses.ServiceTypeResponse;
import com.tourya.api.services.ServiceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de tipos de servicio.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/service-types")
@RequiredArgsConstructor
@Tag(name = "Service Types", description = "API para la gestión de tipos de servicio")
public class ServiceTypeController {

    private final ServiceTypeService serviceTypeService;

    @Operation(
            summary = "Obtener todos los tipos de servicio",
            description = "Obtiene todos los tipos de servicio con paginación"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipos de servicio obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<ServiceTypeResponse>> getAllServiceTypes(
            @Parameter(description = "Parámetros de paginación") Pageable pageable
    ) {
        return ResponseEntity.ok(serviceTypeService.getAllServiceTypes(pageable));
    }

    @Operation(
            summary = "Obtener tipo de servicio por ID",
            description = "Obtiene un tipo de servicio específico por su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo de servicio obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Tipo de servicio no encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceTypeResponse> getServiceTypeById(
            @Parameter(description = "ID del tipo de servicio") @PathVariable Integer id
    ) {
        return ResponseEntity.ok(serviceTypeService.getServiceTypeById(id));
    }

    @Operation(
            summary = "Obtener tipos de servicio activos",
            description = "Obtiene todos los tipos de servicio que están activos"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipos de servicio activos obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/active")
    public ResponseEntity<List<ServiceTypeResponse>> getActiveServiceTypes() {
        return ResponseEntity.ok(serviceTypeService.getActiveServiceTypes());
    }

    @Operation(
            summary = "Obtener tipo de servicio por nombre",
            description = "Obtiene un tipo de servicio específico por su nombre"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo de servicio obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Tipo de servicio no encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/name/{name}")
    public ResponseEntity<ServiceTypeResponse> getServiceTypeByName(
            @Parameter(description = "Nombre del tipo de servicio") @PathVariable String name
    ) {
        return ResponseEntity.ok(serviceTypeService.getServiceTypeByName(name));
    }
}



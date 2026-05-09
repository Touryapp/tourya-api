package com.tourya.api.controller;

import com.tourya.api.models.responses.ServiceResponse;
import com.tourya.api.services.ServiceService;
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
 * Controlador REST para la gestión de servicios.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "API para la gestión de servicios")
public class ServiceController {

    private final ServiceService serviceService;

    @Operation(
            summary = "Obtener todos los servicios",
            description = "Obtiene todos los servicios con paginación"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicios obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<ServiceResponse>> getAllServices(
            @Parameter(description = "Parámetros de paginación") Pageable pageable
    ) {
        return ResponseEntity.ok(serviceService.getAllServices(pageable));
    }

    @Operation(
            summary = "Obtener servicio por ID",
            description = "Obtiene un servicio específico por su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(
            @Parameter(description = "ID del servicio") @PathVariable Integer id
    ) {
        return ResponseEntity.ok(serviceService.getServiceById(id));
    }

    @Operation(
            summary = "Obtener servicios activos",
            description = "Obtiene todos los servicios que están activos"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicios activos obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/active")
    public ResponseEntity<List<ServiceResponse>> getActiveServices() {
        return ResponseEntity.ok(serviceService.getActiveServices());
    }

    @Operation(
            summary = "Obtener servicios por tipo",
            description = "Obtiene todos los servicios de un tipo específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicios obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Tipo de servicio no encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/type/{serviceTypeId}")
    public ResponseEntity<List<ServiceResponse>> getServicesByType(
            @Parameter(description = "ID del tipo de servicio") @PathVariable Integer serviceTypeId
    ) {
        return ResponseEntity.ok(serviceService.getServicesByType(serviceTypeId));
    }

    @Operation(
            summary = "Obtener servicios activos por tipo",
            description = "Obtiene todos los servicios activos de un tipo específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicios activos obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Tipo de servicio no encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/active/type/{serviceTypeId}")
    public ResponseEntity<List<ServiceResponse>> getActiveServicesByType(
            @Parameter(description = "ID del tipo de servicio") @PathVariable Integer serviceTypeId
    ) {
        return ResponseEntity.ok(serviceService.getActiveServicesByType(serviceTypeId));
    }
}
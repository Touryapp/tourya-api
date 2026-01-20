package com.tourya.api.controller;

import com.tourya.api.common.PageResponse;
import com.tourya.api.models.request.MaritimActivityReportRequest;
import com.tourya.api.models.responses.MaritimActivityReportResponse;
import com.tourya.api.services.MaritimActivityReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para gestión de reportes de actividades marítimas DIMAR.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/maritime-activity-reports")
@RequiredArgsConstructor
@Tag(name = "Maritime Activity Reports", description = "API para gestión de reportes DIMAR")
@SecurityRequirement(name = "bearerAuth")
public class MaritimActivityReportController {

    private final MaritimActivityReportService maritimActivityReportService;

    @PostMapping
    @Operation(summary = "Crear reporte DIMAR", description = "Crea un nuevo reporte de actividad marítima")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reporte creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<MaritimActivityReportResponse> create(
            @Valid @RequestBody MaritimActivityReportRequest request,
            Authentication authentication) {
        log.info("Creating maritime activity report");
        
        MaritimActivityReportResponse response = maritimActivityReportService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Obtener todos los reportes", description = "Obtiene todos los reportes DIMAR con paginación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reportes obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<PageResponse<MaritimActivityReportResponse>> findAll(
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") Integer size) {
        log.info("Getting all maritime activity reports - page: {}, size: {}", page, size);
        
        PageResponse<MaritimActivityReportResponse> response = maritimActivityReportService.findAll(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reporte por ID", description = "Obtiene un reporte DIMAR específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte encontrado"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<MaritimActivityReportResponse> findById(
            @Parameter(description = "ID del reporte") @PathVariable Long id) {
        log.info("Getting maritime activity report by id: {}", id);
        
        MaritimActivityReportResponse response = maritimActivityReportService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar reporte", description = "Actualiza un reporte DIMAR existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<MaritimActivityReportResponse> update(
            @Parameter(description = "ID del reporte") @PathVariable Long id,
            @Valid @RequestBody MaritimActivityReportRequest request) {
        log.info("Updating maritime activity report id: {}", id);
        
        MaritimActivityReportResponse response = maritimActivityReportService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar reporte", description = "Elimina un reporte DIMAR por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reporte eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del reporte") @PathVariable Long id) {
        log.info("Deleting maritime activity report id: {}", id);
        
        maritimActivityReportService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/date")
    @Operation(summary = "Buscar reportes por fecha", description = "Obtiene todos los reportes DIMAR de una fecha específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reportes obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<MaritimActivityReportResponse>> findByReportDate(
            @Parameter(description = "Fecha del reporte (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        log.info("Getting maritime activity reports by date: {}", reportDate);
        
        List<MaritimActivityReportResponse> response = maritimActivityReportService.findByReportDate(reportDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/country-city")
    @Operation(summary = "Buscar reportes por país y ciudad", description = "Obtiene todos los reportes DIMAR de un país y ciudad específicos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reportes obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<MaritimActivityReportResponse>> findByCountryAndCity(
            @Parameter(description = "País") @RequestParam String country,
            @Parameter(description = "Ciudad") @RequestParam String city) {
        log.info("Getting maritime activity reports by country: {} and city: {}", country, city);
        
        List<MaritimActivityReportResponse> response = maritimActivityReportService.findByCountryAndCity(country, city);
        return ResponseEntity.ok(response);
    }
}


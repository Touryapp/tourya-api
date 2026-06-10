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

@Slf4j
@RestController
@RequestMapping("/maritime-activity-reports")
@RequiredArgsConstructor
@Tag(name = "Maritime Activity Reports", description = "API para gestión de reportes DIMAR")
@SecurityRequirement(name = "bearerAuth")
public class MaritimActivityReportController {

    private final MaritimActivityReportService maritimActivityReportService;

    @PostMapping
    @Operation(
            summary = "Crear reporte DIMAR",
            description = "Crea un reporte con país/departamento/ciudad por ID, categoría y subcategoría del tour, "
                    + "bandera y rango de fechas (inicio y fin no pueden ser anteriores a hoy).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reporte creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<MaritimActivityReportResponse> create(
            @Valid @RequestBody MaritimActivityReportRequest request,
            Authentication authentication) {
        MaritimActivityReportResponse response = maritimActivityReportService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Obtener todos los reportes", description = "Obtiene todos los reportes DIMAR con paginación")
    public ResponseEntity<PageResponse<MaritimActivityReportResponse>> findAll(
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(maritimActivityReportService.findAll(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reporte por ID")
    public ResponseEntity<MaritimActivityReportResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(maritimActivityReportService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar reporte")
    public ResponseEntity<MaritimActivityReportResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MaritimActivityReportRequest request) {
        return ResponseEntity.ok(maritimActivityReportService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar reporte")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        maritimActivityReportService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/date")
    @Operation(summary = "Buscar reportes vigentes en una fecha")
    public ResponseEntity<List<MaritimActivityReportResponse>> findActiveOnDate(
            @Parameter(description = "Fecha (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        return ResponseEntity.ok(maritimActivityReportService.findActiveOnDate(reportDate));
    }

    @GetMapping("/location")
    @Operation(summary = "Buscar reportes por ubicación (IDs)")
    public ResponseEntity<List<MaritimActivityReportResponse>> findByLocation(
            @RequestParam("countryId") Integer countryId,
            @RequestParam("stateId") Integer stateId,
            @RequestParam("cityId") Integer cityId) {
        return ResponseEntity.ok(maritimActivityReportService.findByLocation(countryId, stateId, cityId));
    }
}

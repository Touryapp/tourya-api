package com.tourya.api.controller;

import com.tourya.api.models.request.TourScheduleConfigCreationRequest;
import com.tourya.api.models.request.TourSearchRequestDto;
import com.tourya.api.models.responses.TourScheduleConfigResponse;
import com.tourya.api.models.responses.TourScheduleSearchResponseDto;
import com.tourya.api.services.TourScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("tour-schedules")
@RequiredArgsConstructor
@Tag(name = "TourSchedule")
public class TourScheduleController {
    private final TourScheduleService tourScheduleService;

    /**
     * Endpoint para crear una nueva configuración de horario de tour
     * y generar automáticamente las instancias de horarios de tour individuales.
     *
     * @param request Datos de la solicitud que incluyen la configuración, slots y precios.
     * @return ResponseEntity con la configuración de horario creada y un estado HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<TourScheduleConfigResponse> createTourSchedule(
            @Valid @RequestBody TourScheduleConfigCreationRequest request,
            Authentication connectedUser) {
        try {
            TourScheduleConfigResponse dto = tourScheduleService.createTourScheduleConfigAndGenerateSchedules(request, connectedUser);
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {

            throw e;
        } catch (Exception e) {
            System.err.println("Error creating tour schedule configuration:" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * Endpoint para modificar una configuración de horario de tour existente
     * y regenerar automáticamente las instancias de horarios de tour individuales.
     *
     * @param configId El ID de la configuración de tour a modificar.
     * @param request Datos de la solicitud con la configuración, slots y precios actualizados.
     * @return ResponseEntity con la configuración de horario actualizada y un estado HTTP 200 OK.
     */
    @PutMapping("/{configId}")
    public ResponseEntity<TourScheduleConfigResponse> updateTourSchedule(
            @PathVariable Integer configId,
            @Valid @RequestBody TourScheduleConfigCreationRequest request,
            Authentication connectedUser) {
        try {
            if (request.getId() != null && !request.getId().equals(configId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The ID in the path doesn't match the ID in the request body.");
            }
            request.setId(configId);
            TourScheduleConfigResponse dto = tourScheduleService.updateTourScheduleConfigAndGenerateSchedules(configId, request, connectedUser);
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error updating tour schedule configuration:" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint para consultar los detalles completos de una configuración de tour,
     * incluyendo sus slots, precios y horarios generados.
     *
     * @param configId El ID de la configuración de tour a consultar.
     * @return ResponseEntity con el DTO de respuesta completo y un estado HTTP 200 OK.
     */
    @GetMapping("/{configId}")
    public ResponseEntity<TourScheduleConfigResponse> getTourScheduleDetails(
            @PathVariable Integer configId) {
        try {
            TourScheduleConfigResponse details = tourScheduleService.getTourScheduleConfigDetails(configId);
            return new ResponseEntity<>(details, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error al obtener los detalles de la configuración de horario del tour: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint para realizar una búsqueda inteligente de tours disponibles para reserva.
     * Permite filtrar por múltiples criterios como palabra clave, categoría, fechas, horas, capacidad, precio y ubicación.
     *
     * @param request DTO con los parámetros de búsqueda.
     * @return Una página de TourScheduleSearchResponseDto que coinciden con los criterios.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TourScheduleSearchResponseDto>> searchTours(
            @ModelAttribute TourSearchRequestDto request) {
        try {
            Page<TourScheduleSearchResponseDto> result = tourScheduleService.searchToursForReservation(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error al realizar la búsqueda de tours: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

package com.tourya.api.controller;

import com.tourya.api.models.TourScheduleConfig;
import com.tourya.api.models.request.TourScheduleConfigCreationRequest;
import com.tourya.api.models.request.TourScheduleRequest;
import com.tourya.api.models.responses.TourScheduleConfigResponseDto;
import com.tourya.api.models.responses.TourScheduleResponse;
import com.tourya.api.services.TourScheduleService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("tour-schedules")
@RequiredArgsConstructor
@Tag(name = "TourSchedule")
public class TourScheduleController {
    private final TourScheduleService tourScheduleService;

    /*@PostMapping("/user/save/{tourId}")
    public ResponseEntity<TourScheduleResponse> save(
            @Valid @RequestBody TourScheduleRequest tourScheduleRequest,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourScheduleService.saveTourScheduleByTourId(tourScheduleRequest, tourId, connectedUser));
    }*/
    /**
     * Endpoint para crear una nueva configuración de horario de tour
     * y generar automáticamente las instancias de horarios de tour individuales.
     *
     * @param request Datos de la solicitud que incluyen la configuración, slots y precios.
     * @return ResponseEntity con la configuración de horario creada y un estado HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<TourScheduleConfigResponseDto> createTourSchedule(
            @Valid @RequestBody TourScheduleConfigCreationRequest request,
            Authentication connectedUser) { // @Valid si añades validaciones en los DTOs
        try {
            TourScheduleConfigResponseDto dto = tourScheduleService.createTourScheduleConfigAndGenerateSchedules(request, connectedUser);
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            // Manejo de errores específicos (ej. tour no encontrado, día de la semana inválido)
            throw e; // Spring Boot se encargará de mapear esto a una respuesta HTTP
        } catch (Exception e) {
            // Manejo de errores genéricos inesperados
            // Idealmente, aquí se registraría el error para depuración
            System.err.println("Error al crear la configuración de horario del tour: " + e.getMessage());
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
    public ResponseEntity<TourScheduleConfigResponseDto> updateTourSchedule(
            @PathVariable Integer configId,
            @Valid @RequestBody TourScheduleConfigCreationRequest request,
            Authentication connectedUser) {
        try {
            // Es buena práctica que el ID del path coincida con el ID del body si existe
            if (request.getId() != null && !request.getId().equals(configId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El ID en el path no coincide con el ID en el cuerpo de la solicitud.");
            }
            // Aseguramos que el ID del request sea el del path para el servicio
            request.setId(configId);

            TourScheduleConfigResponseDto dto = tourScheduleService.updateTourScheduleConfigAndGenerateSchedules(configId, request, connectedUser);
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error al actualizar la configuración de horario del tour: " + e.getMessage());
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
    public ResponseEntity<TourScheduleConfigResponseDto> getTourScheduleDetails(
            @PathVariable Integer configId) {
        try {
            TourScheduleConfigResponseDto details = tourScheduleService.getTourScheduleConfigDetails(configId);
            return new ResponseEntity<>(details, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error al obtener los detalles de la configuración de horario del tour: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

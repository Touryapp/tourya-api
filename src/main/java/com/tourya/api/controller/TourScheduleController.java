package com.tourya.api.controller;

import com.tourya.api.common.PageResponse;
import com.tourya.api.models.Provider;
import com.tourya.api.models.User;
import com.tourya.api.models.request.TourScheduleConfigCreationRequest;
import com.tourya.api.models.request.TourScheduleRequest;
import com.tourya.api.models.request.TourSearchRequestDto;
import com.tourya.api.models.responses.TourScheduleBulkResponse;
import com.tourya.api.models.responses.TourScheduleConfigResponse;
import com.tourya.api.models.responses.TourScheduleResponse;
import com.tourya.api.models.responses.TourScheduleSearchResponseDto;
import com.tourya.api.services.TourConfigTemplateService;
import com.tourya.api.services.TourScheduleConfigGeneralService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("tour-schedules")
@RequiredArgsConstructor
@Tag(name = "TourSchedule")
public class TourScheduleController {
    private final TourScheduleConfigGeneralService tourScheduleConfigGeneralService;
    private final TourConfigTemplateService configTemplateService;

    @GetMapping("/tours/{tourId}")
    public ResponseEntity<PageResponse<TourScheduleResponse>> getAllTourSchedulesByTourId(
            @PathVariable Integer tourId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(tourScheduleConfigGeneralService.findAllByTourId(tourId, page, size, connectedUser));
    }

    /**
     * Endpoint para crear una nueva configuración de horario de tour
     * y generar automáticamente las instancias de horarios de tour individuales.
     *
     * @param isTemplate Indica si la configuración es una plantilla.
     * @param request Datos de la solicitud que incluyen la configuración, slots y precios.
     * @return ResponseEntity con la configuración de horario creada y un estado HTTP 201 Created.
     */
    @PostMapping("/config")
    public ResponseEntity<TourScheduleConfigResponse> createTourSchedule(
            @RequestParam Boolean isTemplate,
            @Valid @RequestBody TourScheduleConfigCreationRequest request,
            Authentication connectedUser) {
        request.setIsTemplate(isTemplate);
        TourScheduleConfigResponse dto = tourScheduleConfigGeneralService.createTourScheduleConfig(request, connectedUser);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }
    /**
     * Endpoint para modificar una configuración de horario de tour existente
     * y regenerar automáticamente las instancias de horarios de tour individuales.
     *
     * @param configId El ID de la configuración de tour a modificar.
     * @param request Datos de la solicitud con la configuración, slots y precios actualizados.
     * @return ResponseEntity con la configuración de horario actualizada y un estado HTTP 200 OK.
     */
    @PutMapping("config/{configId}")
    public ResponseEntity<TourScheduleConfigResponse> updateTourSchedule(
            @PathVariable Integer configId,
            @Valid @RequestBody TourScheduleConfigCreationRequest request,
            Authentication connectedUser) {
        if (request.getId() != null && !request.getId().equals(configId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The ID in the path doesn't match the ID in the request body.");
        }
        request.setId(configId);
        TourScheduleConfigResponse dto = tourScheduleConfigGeneralService.updateTourScheduleConfig(configId, request, connectedUser);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    /**
     * Endpoint para consultar los detalles completos de una configuración de tour,
     * incluyendo sus slots, precios y horarios generados.
     *
     * @param configId El ID de la configuración de tour a consultar.
     * @return ResponseEntity con el DTO de respuesta completo y un estado HTTP 200 OK.
     */
    @GetMapping("config/{configId}")
    public ResponseEntity<TourScheduleConfigResponse> getTourScheduleDetails(
            @PathVariable Integer configId) {
        try {
            TourScheduleConfigResponse details =tourScheduleConfigGeneralService.getTourScheduleConfigDetails(configId);
            return new ResponseEntity<>(details, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error al obtener los detalles de la configuración de horario del tour: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Endpoint para guardar o actualizar en lote los horarios de tour enviados desde el frontend.
     *
     * @param requests Lista de TourScheduleRequest a procesar.
     * @return ResponseEntity con estado 204 No Content si todo fue exitoso.
     */
    @PostMapping("/batch")
    public ResponseEntity<List<TourScheduleBulkResponse>> saveOrUpdateTourSchedulesBulk(
            @Valid @RequestBody java.util.List<TourScheduleRequest> requests, Authentication connectedUser) {
        List<TourScheduleBulkResponse> response = tourScheduleConfigGeneralService.saveOrUpdateTourSchedules(requests, connectedUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<TourScheduleConfigResponse>> getTemplatesByProvider(Authentication connectedUser)  {
        return ResponseEntity.ok(configTemplateService.getConfigTemplatesByProvider(connectedUser));
    }
}

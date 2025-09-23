package com.tourya.api.controller;

import com.tourya.api.models.request.CreateReservationRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.services.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar reservas.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Management", description = "API para gestión de reservas")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Crea una nueva reserva.
     * 
     * @param request Datos de la reserva a crear
     * @return ReservationResponse con la información de la reserva creada
     */
    @PostMapping
    @Operation(summary = "Crear reserva", description = "Crea una nueva reserva después de un pago exitoso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        log.info("Creating reservation for payment: {}", request.getPaymentId());
        
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene una reserva por su ID.
     * 
     * @param id ID de la reserva
     * @return ReservationResponse con la información de la reserva
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener reserva por ID", description = "Obtiene la información de una reserva específica por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> getReservationById(
            @Parameter(description = "ID de la reserva") @PathVariable Long id) {
        log.info("Getting reservation by id: {}", id);
        
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene una reserva por su token QR.
     * 
     * @param qrToken Token QR de la reserva
     * @return ReservationResponse con la información de la reserva
     */
    @GetMapping("/qr/{qrToken}")
    @Operation(summary = "Obtener reserva por token QR", description = "Obtiene la información de una reserva por su token QR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> getReservationByQrToken(
            @Parameter(description = "Token QR de la reserva") @PathVariable String qrToken) {
        log.info("Getting reservation by QR token: {}", qrToken);
        
        ReservationResponse response = reservationService.getReservationByQrToken(qrToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todas las reservas de un pago específico.
     * 
     * @param paymentId ID del pago
     * @return Lista de ReservationResponse
     */
    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "Obtener reservas por pago", description = "Obtiene todas las reservas asociadas a un pago específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByPaymentId(
            @Parameter(description = "ID del pago") @PathVariable Long paymentId) {
        log.info("Getting reservations for payment: {}", paymentId);
        
        List<ReservationResponse> responses = reservationService.getReservationsByPaymentId(paymentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene todas las reservas por estado de entrega.
     * 
     * @param deliveryStatus Estado de entrega
     * @return Lista de ReservationResponse
     */
    @GetMapping("/delivery-status/{deliveryStatus}")
    @Operation(summary = "Obtener reservas por estado de entrega", description = "Obtiene todas las reservas que tienen un estado de entrega específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByDeliveryStatus(
            @Parameter(description = "Estado de entrega") @PathVariable String deliveryStatus) {
        log.info("Getting reservations by delivery status: {}", deliveryStatus);
        
        List<ReservationResponse> responses = reservationService.getReservationsByDeliveryStatus(deliveryStatus);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene todas las reservas de un responsable de servicio.
     * 
     * @param serviceResponsibleId ID del responsable del servicio
     * @return Lista de ReservationResponse
     */
    @GetMapping("/service-responsible/{serviceResponsibleId}")
    @Operation(summary = "Obtener reservas por responsable", description = "Obtiene todas las reservas de un responsable de servicio específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByServiceResponsibleId(
            @Parameter(description = "ID del responsable del servicio") @PathVariable Integer serviceResponsibleId) {
        log.info("Getting reservations for service responsible: {}", serviceResponsibleId);
        
        List<ReservationResponse> responses = reservationService.getReservationsByServiceResponsibleId(serviceResponsibleId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene todas las reservas de un pagador.
     * 
     * @param payerId ID del pagador
     * @return Lista de ReservationResponse
     */
    @GetMapping("/payer/{payerId}")
    @Operation(summary = "Obtener reservas por pagador", description = "Obtiene todas las reservas de un pagador específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByPayerId(
            @Parameter(description = "ID del pagador") @PathVariable Integer payerId) {
        log.info("Getting reservations for payer: {}", payerId);
        
        List<ReservationResponse> responses = reservationService.getReservationsByPayerId(payerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene todas las reservas por email del responsable del servicio.
     * 
     * @param serviceResponsibleEmail Email del responsable del servicio
     * @return Lista de ReservationResponse
     */
    @GetMapping("/service-responsible/email/{serviceResponsibleEmail}")
    @Operation(summary = "Obtener reservas por email del responsable", description = "Obtiene todas las reservas de un responsable de servicio por su email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByServiceResponsibleEmail(
            @Parameter(description = "Email del responsable del servicio") @PathVariable String serviceResponsibleEmail) {
        log.info("Getting reservations for service responsible email: {}", serviceResponsibleEmail);
        
        List<ReservationResponse> responses = reservationService.getReservationsByServiceResponsibleEmail(serviceResponsibleEmail);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene todas las reservas por email del pagador.
     * 
     * @param payerEmail Email del pagador
     * @return Lista de ReservationResponse
     */
    @GetMapping("/payer/email/{payerEmail}")
    @Operation(summary = "Obtener reservas por email del pagador", description = "Obtiene todas las reservas de un pagador por su email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByPayerEmail(
            @Parameter(description = "Email del pagador") @PathVariable String payerEmail) {
        log.info("Getting reservations for payer email: {}", payerEmail);
        
        List<ReservationResponse> responses = reservationService.getReservationsByPayerEmail(payerEmail);
        return ResponseEntity.ok(responses);
    }

    /**
     * Verifica si existe una reserva para un pago específico.
     * 
     * @param paymentId ID del pago
     * @return true si existe una reserva, false en caso contrario
     */
    @GetMapping("/payment/{paymentId}/exists")
    @Operation(summary = "Verificar existencia de reserva", description = "Verifica si existe una reserva para un pago específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación completada")
    })
    public ResponseEntity<Boolean> existsReservationForPayment(
            @Parameter(description = "ID del pago") @PathVariable Long paymentId) {
        log.info("Checking if payment {} has reservation", paymentId);
        
        boolean exists = reservationService.existsReservationForPayment(paymentId);
        return ResponseEntity.ok(exists);
    }

}

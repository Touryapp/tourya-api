package com.tourya.api.controller;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
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

import java.time.LocalDateTime;
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
     * Obtiene una reserva por su URL QR.
     * 
     * @param qrUrl URL QR de la reserva
     * @return ReservationResponse con la información de la reserva
     */
    @GetMapping("/qr/{qrUrl}")
    @Operation(summary = "Obtener reserva por URL QR", description = "Obtiene la información de una reserva por su URL QR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> getReservationByQrUrl(
            @Parameter(description = "URL QR de la reserva") @PathVariable String qrUrl) {
        log.info("Getting reservation by QR URL: {}", qrUrl);
        
        ReservationResponse response = reservationService.getReservationByQrUrl(qrUrl);
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
            @Parameter(description = "Estado de entrega") @PathVariable DeliveryStatusEnum deliveryStatus) {
        log.info("Getting reservations by delivery status: {}", deliveryStatus);
        
        List<ReservationResponse> responses = reservationService.getReservationsByDeliveryStatus(deliveryStatus);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene todas las reservas por fecha de reserva.
     * 
     * @param reservationDate Fecha de reserva
     * @return Lista de ReservationResponse
     */
    @GetMapping("/date/{reservationDate}")
    @Operation(summary = "Obtener reservas por fecha", description = "Obtiene todas las reservas de una fecha específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByReservationDate(
            @Parameter(description = "Fecha de reserva") @PathVariable LocalDateTime reservationDate) {
        log.info("Getting reservations by reservation date: {}", reservationDate);
        
        List<ReservationResponse> responses = reservationService.getReservationsByReservationDate(reservationDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene todas las reservas por rango de fechas de reserva.
     * 
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Lista de ReservationResponse
     */
    @GetMapping("/date-range")
    @Operation(summary = "Obtener reservas por rango de fechas", description = "Obtiene todas las reservas dentro de un rango de fechas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    })
    public ResponseEntity<List<ReservationResponse>> getReservationsByReservationDateRange(
            @Parameter(description = "Fecha de inicio") @RequestParam LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam LocalDateTime endDate) {
        log.info("Getting reservations by date range: {} to {}", startDate, endDate);
        
        List<ReservationResponse> responses = reservationService.getReservationsByReservationDateRange(startDate, endDate);
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

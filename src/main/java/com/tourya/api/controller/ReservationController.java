package com.tourya.api.controller;

import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.models.request.CancelReservationRequest;
import com.tourya.api.models.request.RescheduleReservationRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.models.responses.ReservationDetailsResponse;
import com.tourya.api.models.responses.RescheduleValidationResponse;
import com.tourya.api.models.responses.RescheduleResponse;
import com.tourya.api.services.ReservationService;
import com.tourya.api.services.ReservationQrService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final ReservationQrService reservationQrService;

    @GetMapping
    public ResponseEntity<PageResponse<ReservationDetailsResponse>> getProviderReservations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "providerId", required = false) Integer providerId,
            @RequestParam(name = "reservationId", required = false) Long reservationId,
            @RequestParam(name = "status", required = false) DeliveryStatusEnum status,
            Authentication connectedUser
    ) {

        PageResponse<ReservationDetailsResponse> response =
                reservationService.getProviderReservations(
                        page,
                        size,
                        providerId,
                        reservationId,
                        status,
                        connectedUser
                );

        return ResponseEntity.ok(response);
    }



    // --- OTROS MÉTODOS ---

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

    @PostMapping("/{reservationId}/qr/regenerate")
    @Operation(summary = "Regenerar código QR", description = "Regenera y actualiza el código QR de una reserva")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "QR regenerado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<String> regenerateQrCode(
            @Parameter(description = "ID de la reserva") @PathVariable Long reservationId) {
        log.info("Regenerating QR code for reservation: {}", reservationId);
        String qrUrl = reservationQrService.regenerateQrCode(reservationId);
        return ResponseEntity.ok(qrUrl);
    }

    @DeleteMapping("/{reservationId}/qr")
    @Operation(summary = "Eliminar código QR", description = "Elimina el código QR de una reserva de S3")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "QR eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<Void> deleteQrCode(
            @Parameter(description = "ID de la reserva") @PathVariable Long reservationId) {
        log.info("Deleting QR code for reservation: {}", reservationId);
        reservationQrService.deleteQrCode(reservationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Consume/procesa una reserva, cambiando su estado y creando la cuenta por pagar al proveedor.
     * 
     * @param reservationId ID de la reserva a procesar
     * @return ReservationResponse con la información actualizada
     */
    @PostMapping("/{reservationId}/consume")
    @Operation(summary = "Consumir reserva", description = "Procesa una reserva, cambia su estado a DELIVERED, actualiza el item del carrito a COMPLETED y crea una cuenta por pagar al proveedor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva consumida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "400", description = "La reserva ya fue consumida o hay datos inválidos")
    })
    public ResponseEntity<ReservationResponse> consumeReservation(
            @Parameter(description = "ID de la reserva a consumir") @PathVariable Long reservationId) {
        log.info("Consuming reservation: {}", reservationId);
        
        ReservationResponse response = reservationService.consumeReservation(reservationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela una reserva.
     * 
     * @param reservationId ID de la reserva a cancelar
     * @param request Request con el motivo de cancelación
     * @param authentication Autenticación del usuario
     * @return ReservationResponse con la reserva cancelada
     */
    @PutMapping("/{reservationId}/cancel")
    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva según el motivo proporcionado (no puede asistir o por lluvia)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva cancelada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "400", description = "No se puede cancelar la reserva (validaciones fallidas)")
    })
    public ResponseEntity<ReservationResponse> cancelReservation(
            @Parameter(description = "ID de la reserva a cancelar") @PathVariable Long reservationId,
            @Valid @RequestBody CancelReservationRequest request,
            Authentication authentication) {
        log.info("Canceling reservation {} with reason: {}", reservationId, request.getCancellationReason());
        
        ReservationResponse response = reservationService.cancelReservation(reservationId, request, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Valida si una reserva puede ser re-agendada.
     * 
     * @param reservationId ID de la reserva a validar
     * @param authentication Autenticación del usuario
     * @return RescheduleValidationResponse con el resultado de la validación
     */
    @GetMapping("/{reservationId}/reschedule/validate")
    @Operation(summary = "Validar re-agendamiento", description = "Valida si una reserva puede ser re-agendada según las políticas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validación completada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<RescheduleValidationResponse> validateRescheduleReservation(
            @Parameter(description = "ID de la reserva a validar") @PathVariable Long reservationId,
            Authentication authentication) {
        log.info("Validating reschedule for reservation: {}", reservationId);
        
        RescheduleValidationResponse response = reservationService.validateRescheduleReservation(reservationId, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Re-agenda una reserva con nueva fecha y configuración.
     * 
     * @param reservationId ID de la reserva a re-agendar
     * @param request Request con nueva fecha, configuración de ageType y cantidad
     * @param authentication Autenticación del usuario
     * @return RescheduleResponse con estado de transacción, validación de precio y datos
     */
    @PutMapping("/{reservationId}/reschedule")
    @Operation(summary = "Re-agendar reserva", description = "Re-agenda una reserva con nueva fecha y configuración. Maneja 3 casos: precio igual/menor (actualiza) o mayor (cancela y agrega al carrito)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva re-agendada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "400", description = "No se puede re-agendar la reserva (validaciones fallidas)")
    })
    public ResponseEntity<RescheduleResponse> rescheduleReservation(
            @Parameter(description = "ID de la reserva a re-agendar") @PathVariable Long reservationId,
            @Valid @RequestBody RescheduleReservationRequest request,
            Authentication authentication) {
        log.info("Rescheduling reservation {} to date: {} with new configuration", reservationId, request.getNewDate());
        
        RescheduleResponse response = reservationService.rescheduleReservation(reservationId, request, authentication);
        return ResponseEntity.ok(response);
    }

}

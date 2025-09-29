package com.tourya.api.controller;

import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.responses.PaymentResponse;
import com.tourya.api.services.PaymentService;
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

/**
 * Controlador REST para gestión de pagos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "API para gestión de pagos")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Crear pago", description = "Crea un nuevo pago y automáticamente genera la reserva con sus items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pago y reserva creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Items del carrito no encontrados")
    })
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Creating payment for transaction: {}", request.getTransactionId());
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Obtener pago por ID", description = "Obtiene la información de un pago específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "ID del pago") @PathVariable Long paymentId) {
        log.info("Getting payment by id: {}", paymentId);
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Obtener pago por ID de transacción", description = "Obtiene la información de un pago por su ID de transacción")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(
            @Parameter(description = "ID de transacción") @PathVariable String transactionId) {
        log.info("Getting payment by transaction id: {}", transactionId);
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr/{qrUrl}")
    @Operation(summary = "Obtener URL de imagen QR", description = "Obtiene la URL de la imagen QR almacenada en S3")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL de QR obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "QR no encontrado")
    })
    public ResponseEntity<String> getQrImageUrl(
            @Parameter(description = "URL o token QR") @PathVariable String qrUrl) {
        log.info("Getting QR image URL for: {}", qrUrl);
        String qrImageUrl = paymentService.generateQrImageBase64(qrUrl);
        return ResponseEntity.ok(qrImageUrl);
    }
}
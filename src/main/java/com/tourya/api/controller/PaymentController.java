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

import java.util.List;

/**
 * Controlador REST para gestionar pagos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "API para gestión de pagos")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Crea un nuevo pago.
     * 
     * @param request Datos del pago a crear
     * @return PaymentResponse con la información del pago creado
     */
    @PostMapping
    @Operation(summary = "Crear pago", description = "Crea un nuevo pago para un item del carrito de compras")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pago creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Item del carrito no encontrado")
    })
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Creating payment for shopping cart item: {}", request.getShoppingCartItemId());
        
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene un pago por su ID.
     * 
     * @param id ID del pago
     * @return PaymentResponse con la información del pago
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener pago por ID", description = "Obtiene la información de un pago específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "ID del pago") @PathVariable Long id) {
        log.info("Getting payment by id: {}", id);
        
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene un pago por su ID de transacción.
     * 
     * @param transactionId ID de transacción
     * @return PaymentResponse con la información del pago
     */
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
}

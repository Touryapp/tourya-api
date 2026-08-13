package com.tourya.api.controller;

import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.request.ReserveCreditRequest;
import com.tourya.api.models.request.TransferCreditRequest;
import com.tourya.api.models.responses.TouristLookupResponse;
import com.tourya.api.models.responses.CreditResponse;
import com.tourya.api.models.responses.ReserveCreditResponse;
import com.tourya.api.services.CreditReservationService;
import com.tourya.api.services.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para gestionar créditos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/credits")
@RequiredArgsConstructor
@Tag(name = "Credit Management", description = "API para gestión de créditos")
public class CreditController {

    private final CreditService creditService;
    private final CreditReservationService creditReservationService;

    /**
     * Obtiene todos los créditos del usuario autenticado.
     * Si el usuario es back office, retorna todos los créditos.
     * Si el usuario es normal, retorna solo sus créditos.
     * Opcionalmente puede filtrar por status del crédito.
     * 
     * @param authentication Autenticación del usuario
     * @param status Estado del crédito para filtrar (opcional: CREATED, CANCELED, DELETED)
     * @return Lista de CreditResponse
     */
    @GetMapping
    @Operation(summary = "Obtener todos los créditos", 
               description = "Obtiene los créditos según el rol del usuario (back office: todos, usuario normal: solo los suyos). " +
                           "Opcionalmente puede filtrar por status del crédito (CREATED, CANCELED, DELETED).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de créditos obtenida exitosamente")
    })
    public ResponseEntity<List<CreditResponse>> getAllCredits(
            Authentication authentication,
            @Parameter(description = "Estado del crédito para filtrar (opcional: CREATED, CANCELED, DELETED)")
            @RequestParam(value = "status", required = false) CreditStatusEnum status) {
        log.info("Getting credits for user with status filter: {}", status);
        
        List<CreditResponse> credits = creditService.getAllCredits(authentication, status);
        return ResponseEntity.ok(credits);
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reservar créditos para un item del carrito",
               description = "Reserva el monto indicado sobre los créditos enviados (orden mayor a menor). " +
                             "Los créditos pasan a RESERVED y se asocian al shoppingCartItemId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Créditos reservados correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o créditos insuficientes")
    })
    public ResponseEntity<ReserveCreditResponse> reserveCredits(
            @RequestBody @Valid ReserveCreditRequest request,
            Authentication authentication) {
        log.info("Reserving credits for item {} amount {}", request.getShoppingCartItemId(), request.getAmountToReserve());
        ReserveCreditResponse response = creditReservationService.reserveCredits(request, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tourist-lookup")
    @Operation(summary = "Buscar turista por documento", description = "Retorna userId para usar en transferencia de crédito.")
    public ResponseEntity<TouristLookupResponse> lookupTouristByDocument(
            @Parameter(description = "Número de documento del turista destino", required = true)
            @RequestParam("documentNumber") String documentNumber) {
        return ResponseEntity.ok(creditService.lookupTouristByDocument(documentNumber));
    }

    @PostMapping("/{creditId}/transfer")
    @Operation(summary = "Transferir crédito a otro turista", description = "Solo una transferencia por crédito.")
    public ResponseEntity<CreditResponse> transferCredit(
            @PathVariable Long creditId,
            @RequestBody @Valid TransferCreditRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(creditService.transferCredit(creditId, request, authentication));
    }

    @PostMapping("/{creditId}/request-refund")
    @Operation(
            operationId = "touristRequestCreditRefund",
            summary = "Turista solicita la devolucion en efectivo del credito (TC-022 #253)",
            description = "Transicion CREATED -> REFUND_REQUESTED. Guards: credito propio, "
                    + "status actual CREATED, saldo libre (amount - reservedAmount) > 0, no vencido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devolucion solicitada"),
            @ApiResponse(responseCode = "400", description = "Transicion no permitida"),
            @ApiResponse(responseCode = "404", description = "Credito no existe")
    })
    public ResponseEntity<CreditResponse> requestRefund(
            @PathVariable("creditId") Long creditId,
            Authentication authentication) {
        return ResponseEntity.ok(creditService.requestRefund(creditId, authentication));
    }
}

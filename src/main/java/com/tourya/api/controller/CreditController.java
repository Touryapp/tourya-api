package com.tourya.api.controller;

import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.responses.CreditResponse;
import com.tourya.api.services.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestParam(required = false) CreditStatusEnum status) {
        log.info("Getting credits for user with status filter: {}", status);
        
        List<CreditResponse> credits = creditService.getAllCredits(authentication, status);
        return ResponseEntity.ok(credits);
    }
}

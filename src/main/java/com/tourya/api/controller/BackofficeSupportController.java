package com.tourya.api.controller;

import com.tourya.api.agents.backoffice.BackofficeSupportService;
import com.tourya.api.agents.backoffice.DimarDraftResponse;
import com.tourya.api.agents.backoffice.KybChecklistResponse;
import com.tourya.api.agents.backoffice.PayoutAnomalyResponse;
import com.tourya.api.agents.backoffice.TourPrevalidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * IA-08: endpoints REST del agente Backoffice Support. Cuatro capabilities
 * action-specific admin — cada una expone un caso de uso puntual del backoffice
 * (KYB, tour, DIMAR, payouts).
 *
 * <p>Todos requieren JWT + rol ADMIN o BACKOFFICE_OPERATION — el guard vive en
 * {@link BackofficeSupportService}, mismo patron que IA-11
 * ({@code AgentObservabilityService}). Rol insuficiente lanza
 * {@link com.tourya.api.exceptions.InsufficientPrivilegesException} que el
 * {@code GlobalExceptionHandler} traduce a 401.</p>
 */
@RestController
@RequestMapping("/admin/agents/backoffice-support")
@RequiredArgsConstructor
@Tag(name = "Admin Backoffice Support (IA-08)",
        description = "Agente 5 — asistente al backoffice para KYB + prevalidacion "
                + "de tours + borrador DIMAR + conciliacion payouts. Solo ADMIN o "
                + "BACKOFFICE_OPERATION.")
@SecurityRequirement(name = "bearerAuth")
public class BackofficeSupportController {

    private final BackofficeSupportService service;

    @Operation(
            operationId = "backofficeSupportKybChecklist",
            summary = "Genera checklist KYB pre-verificado (IA-08)",
            description = "Cruza los documentos subidos por el operador contra el catalogo obligatorio "
                    + "(RN-045). Devuelve status por item + overallStatus + reasoning. "
                    + "El agente NUNCA aprueba: RN-010 + RN-046 reservan la aprobacion a ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checklist retornado"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION"),
            @ApiResponse(responseCode = "404", description = "RequestProvider no existe")
    })
    @PostMapping("/kyb-checklist/{requestProviderId}")
    public ResponseEntity<KybChecklistResponse> kybChecklist(
            @PathVariable Integer requestProviderId,
            Authentication authentication) {
        return ResponseEntity.ok(service.kybChecklist(requestProviderId, authentication));
    }

    @Operation(
            operationId = "backofficeSupportTourPrevalidation",
            summary = "Pre-valida un tour antes de acceptTourById (IA-08)",
            description = "Verifica RN-011 (espanol obligatorio), RN-013 (galeria) y politica de "
                    + "cancelacion definida. La logica es deterministica; el LLM aporta solo el "
                    + "reasoning en lenguaje natural para el ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pre-validacion retornada"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION"),
            @ApiResponse(responseCode = "404", description = "Tour no existe")
    })
    @PostMapping("/tour-prevalidation/{tourId}")
    public ResponseEntity<TourPrevalidationResponse> tourPrevalidation(
            @PathVariable Integer tourId,
            Authentication authentication) {
        return ResponseEntity.ok(service.tourPrevalidation(tourId, authentication));
    }

    @Operation(
            operationId = "backofficeSupportDimarDraft",
            summary = "Borrador de manifiesto DIMAR para un provider + fecha (IA-08)",
            description = "Agregacion estructurada sin LLM (RN-054 es 100% manual). Lista los "
                    + "pasajeros de las reservas CONFIRMED/DELIVERED del dia. Un humano revisa "
                    + "y sube el manifiesto — el agente solo pre-arma."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Borrador retornado"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    @PostMapping("/dimar-draft")
    public ResponseEntity<DimarDraftResponse> dimarDraft(
            @Parameter(description = "Fecha del zarpe (YYYY-MM-DD, UTC).")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Provider destinatario.")
            @RequestParam("providerId") Integer providerId,
            Authentication authentication) {
        return ResponseEntity.ok(service.dimarDraft(date, providerId, authentication));
    }

    @Operation(
            operationId = "backofficeSupportPayoutAnomalies",
            summary = "Detecta anomalias en payouts vs AccountPayable (IA-08)",
            description = "Concilia cada ProviderPayoutOrder del rango contra sus AccountPayable "
                    + "esperados (RN-042: amount al operador = providerPrice x quantity). El agente "
                    + "SOLO senala — jamas modifica montos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Anomalias retornadas (lista vacia = todo OK)"),
            @ApiResponse(responseCode = "401", description = "Falta rol ADMIN o BACKOFFICE_OPERATION")
    })
    @PostMapping("/payout-anomalies")
    public ResponseEntity<List<PayoutAnomalyResponse>> payoutAnomalies(
            @Parameter(description = "Desde (YYYY-MM-DD, UTC, inclusive).")
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (YYYY-MM-DD, UTC, inclusive).")
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return ResponseEntity.ok(service.payoutAnomalies(from, to, authentication));
    }
}

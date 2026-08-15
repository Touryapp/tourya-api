package com.tourya.api.controller;

import com.tourya.api.agents.operator.DraftReviewReplyResponse;
import com.tourya.api.agents.operator.OperatorSupportService;
import com.tourya.api.agents.operator.PriceAlert;
import com.tourya.api.agents.operator.SuggestTourContentRequest;
import com.tourya.api.agents.operator.TourContentSuggestion;
import com.tourya.api.agents.operator.ValidateGalleryRequest;
import com.tourya.api.agents.operator.ValidateGalleryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IA-07: endpoints REST del agente Operator Support. Cuatro capabilities
 * action-specific (no chat unificado) — cada una expone un caso de uso puntual
 * del wizard de tour + gestion de reseñas del provider.
 *
 * <p>Todos los endpoints requieren JWT con rol PROVIDER o PROVIDER_OPERATOR.
 * La autorizacion owner-based (tour/review pertenece al provider del user) se
 * hace en el service — este controller no repite la logica.</p>
 */
@RestController
@RequestMapping("/agents/operator-support")
@RequiredArgsConstructor
@Tag(name = "Operator Support (IA-07)",
        description = "Agente 4 — asistencia al operador para redaccion de tours + pricing + reseñas")
@SecurityRequirement(name = "bearerAuth")
public class OperatorSupportController {

    private final OperatorSupportService operatorSupportService;

    @Operation(
            operationId = "operatorSupportSuggestTourContent",
            summary = "Sugerir nombre + descripcion + tags SEO para un tour del wizard",
            description = "Genera un borrador estructurado que el operador aprueba antes de "
                    + "PUT /tour/user/submitTourById/{id} (RN-014). No publica automatico."
    )
    @PostMapping("/suggest-tour-content")
    public ResponseEntity<TourContentSuggestion> suggestTourContent(
            @Valid @RequestBody SuggestTourContentRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(operatorSupportService.suggestTourContent(request, connectedUser));
    }

    @Operation(
            operationId = "operatorSupportPriceAlert",
            summary = "Analizar si el precio del tour esta alineado con comparables",
            description = "Devuelve severity + rango de precios de tours de la misma subcategoria. "
                    + "RN-014: solo informa, jamas modifica providerPrice."
    )
    @PostMapping("/price-alert/{tourId}")
    public ResponseEntity<PriceAlert> priceAlert(
            @PathVariable Integer tourId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(operatorSupportService.priceAlert(tourId, connectedUser));
    }

    @Operation(
            operationId = "operatorSupportDraftReviewReply",
            summary = "Redactar borrador de respuesta a una reseña",
            description = "Devuelve draft + tono + idioma. El operador aprueba antes de "
                    + "PATCH /public/save/review/{reviewId}. No publica automatico."
    )
    @PostMapping("/draft-review-reply/{reviewId}")
    public ResponseEntity<DraftReviewReplyResponse> draftReviewReply(
            @PathVariable Long reviewId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(operatorSupportService.draftReviewReply(reviewId, connectedUser));
    }

    @Operation(
            operationId = "operatorSupportValidateGallery",
            summary = "Validar metadata de galeria antes del upload (RN-013)",
            description = "Cero costo LLM. Devuelve issues por imagen + suggestions generales. "
                    + "El frontend evita subir bytes que van a ser rechazados por GalleryValidator."
    )
    @PostMapping("/validate-gallery")
    public ResponseEntity<ValidateGalleryResponse> validateGallery(
            @Valid @RequestBody ValidateGalleryRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(operatorSupportService.validateGallery(request, connectedUser));
    }
}

package com.tourya.api.controller;

import com.tourya.api._utils.Utils;
import com.tourya.api.agents.moderation.ModerationResult;
import com.tourya.api.agents.moderation.ReviewModerationService;
import com.tourya.api.agents.moderation.ReviewModerationSummaryDto;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.models.User;
import com.tourya.api.services.AdminReviewModerationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * IA-10: endpoints admin para el Agente 6 (Moderacion de reseñas).
 *
 * <ul>
 *   <li>{@code POST /admin/agents/review-moderation/{reviewId}/re-moderate} —
 *       re-corre el agente sobre una reseña (backfill de reseñas legacy sin
 *       moderar, testing end-to-end, recovery si el listener AFTER_COMMIT
 *       fallo).</li>
 *   <li>{@code GET /admin/reviews/moderation?status=...} — lista reseñas
 *       flaggeadas para que el backoffice priorice la revision manual.</li>
 * </ul>
 *
 * <p>Ambos requieren JWT + rol {@code ADMIN} o {@code BACKOFFICE_OPERATION} —
 * mismo patron que {@code AgentObservabilityController} (IA-11) y
 * {@code AdminCreditController} (TC-022 #253).</p>
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Admin Review Moderation (IA-10)",
        description = "Agente 6 — moderacion asistida de reseñas del turista (auto-flag, nunca borra)")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewModerationController {

    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    private final ReviewModerationService moderationService;
    private final AdminReviewModerationQueryService queryService;

    @Operation(
            operationId = "adminReMoDerateReview",
            summary = "Re-correr el agente de moderacion sobre una reseña existente",
            description = "Backfill de reseñas legacy sin moderar, testing end-to-end o recovery si "
                    + "el listener AFTER_COMMIT del createReview fallo. Solo ADMIN o BACKOFFICE_OPERATION. "
                    + "Sincrono — la respuesta HTTP contiene el veredicto."
    )
    @PostMapping("/admin/agents/review-moderation/{reviewId}/re-moderate")
    public ResponseEntity<ModerationResult> reModerate(
            @PathVariable Long reviewId,
            Authentication authentication) {
        requireBackofficeRole(authentication);
        log.info("IA-10 admin re-moderate triggered reviewId={} user={}",
                reviewId, principalIdOrNull(authentication));
        return ResponseEntity.ok(moderationService.moderate(reviewId));
    }

    @Operation(
            operationId = "adminListModerationQueue",
            summary = "Listar reseñas flaggeadas por el agente 6 para revision manual",
            description = "Filtra por moderation_status (PENDING | REJECTED). Devuelve reviewText "
                    + "truncado a 200 chars. Solo ADMIN o BACKOFFICE_OPERATION."
    )
    @GetMapping("/admin/reviews/moderation")
    public ResponseEntity<List<ReviewModerationSummaryDto>> listModerationQueue(
            Authentication authentication,
            @Parameter(description = "PENDING | REJECTED (default PENDING). APPROVED se ignora — no aporta a la cola.")
            @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Desde (YYYY-MM-DD). Default: hoy - 30d.")
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (YYYY-MM-DD). Default: hoy.")
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Limit (default 50, max 200).")
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        requireBackofficeRole(authentication);
        return ResponseEntity.ok(queryService.listQueue(status, from, to, limit));
    }

    private void requireBackofficeRole(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
        if (!Utils.isTouryaBackoffice(user.getRoles())) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    private static Integer principalIdOrNull(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        return principal instanceof User u ? u.getId() : null;
    }
}

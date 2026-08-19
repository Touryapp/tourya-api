package com.tourya.api.agents.moderation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * IA-10: escucha {@link ReviewModerationEvent} DESPUES del commit de la tx que
 * crea la reseña y delega al metodo del {@link ReviewModerationService} que
 * corre en background.
 *
 * <p>Sin {@code @Async} en el listener — {@link ReviewModerationService#moderateAsync(Long)}
 * ya lo esta, asi que la carga de calls al LLM se despacha al executor sin
 * bloquear la respuesta HTTP del {@code createReview}. Se evita el bug
 * observado en {@code MaritimeAlertEventListener} (async + AFTER_COMMIT no
 * disparaba en dev). Mismo patron que
 * {@link com.tourya.api.services.translation.TourTranslationEventListener}
 * (IA-09) y {@link com.tourya.api.services.credit.events.CreditRefundEventListener}
 * (TC-022).</p>
 *
 * <p>AFTER_COMMIT garantiza que:</p>
 * <ul>
 *   <li>Si la tx del save hace rollback, el agente no dispara — evita moderar
 *       algo que no existe.</li>
 *   <li>La reseña ya esta persistida cuando el moderador la lee de la BD —
 *       {@link ReviewModerationApplier#applyAsync} puede hacer
 *       {@code save()} directamente sin race condition.</li>
 *   <li>Si la moderacion falla, la reseña ya quedo PUBLISHED por RN-050 — el
 *       backoffice puede correr {@code POST /admin/agents/review-moderation/{id}/re-moderate}
 *       para reintentar.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewModerationEventListener {

    private final ReviewModerationService moderationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ReviewModerationEvent event) {
        if (event == null || event.reviewId() == null) {
            log.debug("IA-10 ReviewModerationEvent without reviewId — ignored");
            return;
        }
        log.debug("IA-10 ReviewModerationEvent received reviewId={} — dispatching async",
                event.reviewId());
        moderationService.moderateAsync(event.reviewId());
    }
}

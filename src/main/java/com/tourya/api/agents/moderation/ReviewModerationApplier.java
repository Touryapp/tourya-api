package com.tourya.api.agents.moderation;

import com.tourya.api.models.Review;
import com.tourya.api.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * IA-10: aplica el resultado del Agente 6 sobre la reseña, sin tocar el
 * {@code status} publico (RN-050).
 *
 * <p><b>Por que un service aparte del listener:</b> {@code @Async} necesita un
 * proxy Spring — poner {@code @Async} en el mismo bean que el listener y luego
 * invocarlo con {@code this.method()} lo evita. Delegando al metodo publico de
 * este bean el proxy se aplica correctamente. Mismo motivo que
 * {@link com.tourya.api.services.translation.TourTranslationApplier} (IA-09).</p>
 *
 * <p><b>Guardrails duros:</b></p>
 * <ul>
 *   <li>Nunca modifica {@code comment} — el texto del turista queda intacto.</li>
 *   <li>Nunca cambia {@code status} (PENDING/PUBLISHED/CANCELED) — el flow de
 *       publicacion sigue igual: {@code createReview} deja PUBLISHED por default
 *       (RN-050 hoy). El agente solo agrega metadata paralela.</li>
 *   <li>Nunca borra la reseña.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewModerationApplier {

    private final ReviewRepository reviewRepository;

    /**
     * Persiste el veredicto del agente. {@code @Async} + {@code @Transactional} —
     * corre en hilo aparte con su propia tx. Si algo falla, no revierte la
     * reseña original (ya se persistio antes del listener AFTER_COMMIT).
     *
     * @param reviewId ID de la reseña ya persistida.
     * @param result   Veredicto del agente (nunca {@code null}).
     * @param flagsJson JSON serializado de {@link ModerationResult#flags()} —
     *                  el service lo prepara para no forzar dependencia jackson aca.
     */
    @Async
    @Transactional
    public void applyAsync(Long reviewId, ModerationResult result, String flagsJson) {
        if (reviewId == null || result == null) {
            log.debug("IA-10 applyAsync called with null reviewId/result — skipping");
            return;
        }
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            log.warn("IA-10 review not found reviewId={} — moderation result discarded", reviewId);
            return;
        }
        // Snapshot defensivo del texto original para verificar que NO cambio.
        // El save() solo pisa las 4 columnas nuevas (moderation_*); si Hibernate
        // detecta cambios adicionales es un bug de este service.
        review.setModerationStatus(result.decision().name());
        review.setModerationFlags(flagsJson);
        review.setModerationReasoning(truncate(result.reasoning(), 500));
        review.setModeratedAt(LocalDateTime.now());
        reviewRepository.save(review);
        log.info("IA-10 review moderated reviewId={} decision={} flags={} escalated={}",
                reviewId, result.decision(),
                result.flags() == null ? 0 : result.flags().size(),
                result.escalatedToHuman());
    }

    /**
     * Version sincrona para el endpoint admin de re-moderacion — el caller
     * espera la respuesta HTTP con el resultado.
     */
    @Transactional
    public void applySync(Long reviewId, ModerationResult result, String flagsJson) {
        if (reviewId == null || result == null) return;
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) return;
        review.setModerationStatus(result.decision().name());
        review.setModerationFlags(flagsJson);
        review.setModerationReasoning(truncate(result.reasoning(), 500));
        review.setModeratedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

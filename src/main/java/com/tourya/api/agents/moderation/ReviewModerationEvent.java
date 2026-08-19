package com.tourya.api.agents.moderation;

/**
 * IA-10: evento de dominio publicado por {@link com.tourya.api.services.ReviewService}
 * despues de un {@code createReview} exitoso. Dispara la moderacion automatica
 * (Agente 6) que evalua el texto de la reseña + medios adjuntos y devuelve un
 * veredicto (APPROVED | PENDING | REJECTED) mas flags (SPAM, OFFENSIVE, ...).
 *
 * <p>Consumido por {@link ReviewModerationEventListener} con
 * {@code @TransactionalEventListener(AFTER_COMMIT)} — el save de la reseña
 * responde 200 al turista SIN esperar al call del LLM; el veredicto se rellena
 * en background y se persiste con {@link ReviewModerationApplier}. Mismo patron
 * que {@link com.tourya.api.services.translation.TourTranslationEvent} (IA-09).
 * </p>
 *
 * <p>Contract RN-050: el agente <b>solo flaggea</b> — nunca cambia el
 * {@code status} publico de la reseña (que sigue siendo PUBLISHED por default),
 * nunca borra ni modifica el texto ni contacta al turista. El resultado se lee
 * solo desde el backoffice.</p>
 *
 * @param reviewId ID de la reseña recien persistida.
 */
public record ReviewModerationEvent(Long reviewId) {
}

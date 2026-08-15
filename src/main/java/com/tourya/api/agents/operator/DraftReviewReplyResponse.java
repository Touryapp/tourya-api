package com.tourya.api.agents.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * IA-07: borrador de respuesta a una reseña generado por
 * {@link OperatorSupportService#draftReviewReply}.
 *
 * <p>El backend NUNCA publica esta respuesta — la devuelve al frontend, el
 * operador aprueba/edita, y despues llama a
 * {@code PATCH /public/save/review/{reviewId}} (endpoint existente,
 * {@link com.tourya.api.services.ReviewService#updateReview}).</p>
 *
 * @param draftText        Texto sugerido en el idioma detectado de la reseña.
 * @param detectedLocale   ISO 639-1 del idioma detectado (es/en/pt).
 * @param tone             Tono elegido segun el rating + contenido.
 * @param reasoning        Por que se eligio este tono/enfoque.
 * @param escalatedToHuman {@code true} si el LLM no pudo generar el borrador
 *                         (ej. reseña con lenguaje toxico, budget agotado).
 */
@Builder
@Schema(description = "Borrador de respuesta a reseña (operador aprueba antes de publicar)")
public record DraftReviewReplyResponse(
        String draftText,
        String detectedLocale,
        Tone tone,
        String reasoning,
        boolean escalatedToHuman
) {

    public enum Tone {
        /** Profesional neutro — para reseñas 4-5 estrellas de agradecimiento. */
        PROFESSIONAL,
        /** Calido y afectivo — para reseñas 5 estrellas con detalle emocional. */
        WARM,
        /** Disculpas + acciones correctivas — para reseñas 1-3 estrellas. */
        APOLOGETIC
    }
}

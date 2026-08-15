package com.tourya.api.agents.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * IA-07: sugerencia estructurada devuelta por
 * {@link OperatorSupportService#suggestTourContent}.
 *
 * <p>El operador ve estas propuestas en el UI y las aprueba, edita o descarta
 * antes del {@code PUT /tour/user/submitTourById/{id}} (RN-014 — el operador
 * nunca pierde control).</p>
 *
 * @param nameSuggestions      3 nombres alternativos sugeridos, orden preferido.
 * @param descriptionSuggestion Descripcion SEO-friendly 200-400 palabras en espanol.
 * @param tagSuggestions       5-10 tags recomendados del catalogo {@code tags}.
 * @param reasoning            Explicacion corta al operador de por que estas sugerencias.
 * @param escalatedToHuman     {@code true} cuando el guardrail bloqueo o el LLM no respondio.
 */
@Builder
@Schema(description = "Sugerencia de contenido para el wizard de tour (draft, no publica solo)")
public record TourContentSuggestion(
        List<String> nameSuggestions,
        String descriptionSuggestion,
        List<String> tagSuggestions,
        String reasoning,
        boolean escalatedToHuman
) {}

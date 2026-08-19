package com.tourya.api.agents.moderation;

import lombok.Builder;

import java.util.List;

/**
 * IA-10: resultado del Agente 6 (Moderacion de reseñas).
 *
 * @param decision           Veredicto del agente:
 *                           <ul>
 *                             <li>{@code APPROVED} — reseña limpia, sin flags.</li>
 *                             <li>{@code PENDING} — dudoso; requiere revision humana en backoffice.</li>
 *                             <li>{@code REJECTED} — auto-flag con razon (spam / offensive / ...). El agente NO cambia
 *                                 el {@code status} publico de la reseña, solo persiste este veredicto.</li>
 *                           </ul>
 * @param flags              Lista de categorias detectadas
 *                           ({@code SPAM | OFFENSIVE | OFF_TOPIC | POTENTIAL_FRAUD | INAPPROPRIATE_MEDIA}).
 *                           Vacio para {@code APPROVED}.
 * @param reasoning          Explicacion corta (max ~500 chars) que el LLM genero. Solo para
 *                           revision humana; nunca se muestra al turista.
 * @param escalatedToHuman   {@code true} cuando el agente no pudo decidir (LLM error, JSON malformado,
 *                           budget exhausto, deny-list) y devolvio {@code PENDING} conservador para que
 *                           backoffice revise manualmente.
 */
@Builder
public record ModerationResult(
        Decision decision,
        List<String> flags,
        String reasoning,
        boolean escalatedToHuman
) {

    public enum Decision {
        APPROVED,
        PENDING,
        REJECTED
    }

    /** Flag categories — dominio cerrado del LLM output. */
    public static final class Flag {
        public static final String SPAM = "SPAM";
        public static final String OFFENSIVE = "OFFENSIVE";
        public static final String OFF_TOPIC = "OFF_TOPIC";
        public static final String POTENTIAL_FRAUD = "POTENTIAL_FRAUD";
        public static final String INAPPROPRIATE_MEDIA = "INAPPROPRIATE_MEDIA";

        private Flag() {}
    }
}

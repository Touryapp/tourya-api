package com.tourya.api.agents.concierge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * IA-02: request de {@code POST /agents/travel-concierge/chat}.
 *
 * @param sessionId   Identificador de la conversacion multi-turno. Se persiste
 *                    en {@code agent_audit_log.metadata->>'session_id'} para
 *                    reconstruir el hilo + aplicar el guard de "3+ pagos
 *                    fallidos → fraud_suspected". El cliente lo genera (UUID)
 *                    en el primer mensaje y lo mantiene por sesion.
 * @param userMessage Texto del turista (es / en / pt — el LLM detecta y responde
 *                    en el mismo idioma).
 * @param locale      Sugerencia de idioma explicita ({@code es}, {@code en},
 *                    {@code pt}). Opcional — si viene, se prioriza sobre la
 *                    autodeteccion. Fallback {@code es}.
 * @param tourId      ID del tour cuando la conversacion ya identificó uno
 *                    (para que el agente traiga la ficha completa al contexto).
 *                    Opcional.
 * @param cartId      ID del carrito activo del turista cuando aplique.
 *                    Opcional — el agente puede resolverlo via userMessage +
 *                    getUserActiveCart si no viene.
 */
@Data
public class ConciergeChatRequest {

    @NotBlank(message = "sessionId es obligatorio")
    @Size(max = 128)
    private String sessionId;

    @NotBlank(message = "userMessage no puede estar vacio")
    @Size(max = 2000, message = "userMessage no puede superar 2000 caracteres")
    private String userMessage;

    @Size(max = 4)
    private String locale;

    private Integer tourId;
    private Long cartId;
}

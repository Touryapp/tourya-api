package com.tourya.api.agents.concierge;

import lombok.Builder;

import java.util.List;

/**
 * IA-02: response de {@code POST /agents/travel-concierge/chat}.
 *
 * @param assistantMessage  Texto en lenguaje natural para el turista (respuesta
 *                          final del loop de function calling). Puede venir
 *                          vacio cuando {@code escalatedToHuman=true}.
 * @param actionsExecuted   Lista de function calls que el agente disparo en el
 *                          loop (search_tours, get_tour_detail, add_to_cart,
 *                          get_cart) con su input parseado.
 * @param fraudSuspected    Si el guard detecto 3+ pagos fallidos en la
 *                          sesion. NUNCA se le muestra al turista — solo lo
 *                          expone la API para que el frontend/BFF pueda
 *                          taggear la sesion. Persistido en el audit log.
 * @param escalatedToHuman  {@code true} cuando el guardrail bloqueo la
 *                          request (intento de leak de secretos, o error
 *                          irrecuperable) y el agente delega a un humano.
 * @param sessionId         Eco del sessionId para conveniencia del cliente.
 */
@Builder
public record ConciergeChatResponse(
        String assistantMessage,
        List<ConciergeAction> actionsExecuted,
        boolean fraudSuspected,
        boolean escalatedToHuman,
        String sessionId
) {}

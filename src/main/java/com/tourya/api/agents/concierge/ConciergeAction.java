package com.tourya.api.agents.concierge;

import java.util.Map;

/**
 * IA-02: registro de una accion (function call) ejecutada por el agente
 * Travel Concierge en el loop de tool use.
 *
 * @param name    Nombre canonico de la funcion: {@code search_tours},
 *                {@code get_tour_detail}, {@code add_to_cart}, {@code get_cart}.
 * @param input   Argumentos parseados del LLM antes de ejecutar.
 * @param success {@code true} si el service subyacente respondio OK.
 * @param error   Mensaje del error cuando {@code success=false}. {@code null}
 *                en exito. Nunca contiene stack ni PII.
 */
public record ConciergeAction(
        String name,
        Map<String, Object> input,
        boolean success,
        String error
) {}

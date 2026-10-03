package com.tourya.api.agents.concierge;

import com.fasterxml.jackson.databind.JsonNode;

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
 * @param result  Issue #39 TCM-024 (Luis 2026-10-03): payload del resultado de
 *                la funcion, ya scrubbed de campos sensibles (providerPrice,
 *                slotPercentageTourya). Se inyecta al prompt del proximo
 *                iteration para que el LLM pueda razonar sobre los datos
 *                reales en vez de alucinar. Antes de v2 este campo no
 *                existia — por eso el agente inventaba tours.
 */
public record ConciergeAction(
        String name,
        Map<String, Object> input,
        boolean success,
        String error,
        JsonNode result
) {
    /** Backward-compat constructor sin result (para llamadas que no retornan payload). */
    public ConciergeAction(String name, Map<String, Object> input, boolean success, String error) {
        this(name, input, success, error, null);
    }
}

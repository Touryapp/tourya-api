package com.tourya.api.agents.backoffice;

import lombok.Builder;

import java.util.List;

/**
 * IA-08: expediente KYB pre-verificado. El agente 5 (Backoffice Support) genera
 * el checklist a partir de {@code request_provider_gallery} + catalogo obligatorio
 * de {@link com.tourya.api.models.RequestProviderDocumentType}. El ADMIN toma la
 * decision final de pre-approve/approve — el agente NUNCA la toma sola
 * (RN-010 + RN-046).
 *
 * @param requestProviderId  Id del {@code RequestProvider} evaluado.
 * @param overallStatus      COMPLETE | INCOMPLETE | REJECTED — resumen de la revision.
 * @param items              Un renglon por documento obligatorio.
 * @param reasoning          Explicacion breve del agente (LLM) para el ADMIN.
 * @param escalatedToHuman   {@code true} cuando el LLM fallo o el input es sospechoso.
 */
@Builder
public record KybChecklistResponse(
        Integer requestProviderId,
        String overallStatus,
        List<KybChecklistItem> items,
        String reasoning,
        boolean escalatedToHuman
) {
    public static final String STATUS_COMPLETE = "COMPLETE";
    public static final String STATUS_INCOMPLETE = "INCOMPLETE";
    public static final String STATUS_REJECTED = "REJECTED";
}

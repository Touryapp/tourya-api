package com.tourya.api.agents.backoffice;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * IA-08: anomalia detectada en una {@link com.tourya.api.models.ProviderPayoutOrder}
 * al conciliarla contra {@link com.tourya.api.models.AccountPayable} (RN-042).
 *
 * <p>El agente <b>solo senala</b> — jamas corrige {@code amount},
 * {@code providerPrice}, ni {@code slotPercentageTourya}. Un falso positivo
 * autoresuelto sobre el dinero del operador es un riesgo que no vale.</p>
 *
 * @param payoutOrderId      Orden de pago analizada.
 * @param providerId         Provider destinatario.
 * @param payDate            Fecha planificada de pago.
 * @param amountTotalOrder   {@code provider_payout_order.amount_total} tal cual esta.
 * @param items              Detalle de discrepancias linea por linea.
 * @param explanation        Explicacion natural (LLM opcional). Sin LLM, se usa
 *                           un template deterministico.
 * @param severity           WARN | CRITICAL — el ADMIN prioriza revision.
 */
@Builder
public record PayoutAnomalyResponse(
        Long payoutOrderId,
        Integer providerId,
        LocalDate payDate,
        BigDecimal amountTotalOrder,
        List<PayoutAnomalyItem> items,
        String explanation,
        String severity
) {
    public static final String SEVERITY_WARN = "WARN";
    public static final String SEVERITY_CRITICAL = "CRITICAL";
}

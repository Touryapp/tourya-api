package com.tourya.api.agents.backoffice;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * IA-08: detalle de una desviacion puntual dentro de una {@link PayoutAnomalyResponse}
 * agrupada por {@link com.tourya.api.models.ProviderPayoutOrder}. Se genera cuando
 * el {@code amount} de una linea del payout no coincide con el {@code amount}
 * registrado en la {@link com.tourya.api.models.AccountPayable} enlazada
 * (RN-042: {@code amount al operador = providerPrice x quantity}).
 *
 * @param reservationId      Reserva involucrada.
 * @param accountPayableId   AccountPayable enlazado (puede ser {@code null} si no hay linkage).
 * @param expectedAmount     Valor de {@code account_payable.amount} (RN-042).
 * @param actualAmount       Valor de {@code provider_payout_order_reservation.amount}.
 * @param discrepancy        Diferencia absoluta actual - esperado (positivo = paga de mas).
 * @param code               Codigo estable: MISMATCH | MISSING_ACCOUNT_PAYABLE | TOTAL_DRIFT.
 */
@Builder
public record PayoutAnomalyItem(
        Long reservationId,
        Long accountPayableId,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal discrepancy,
        String code
) {
    public static final String CODE_MISMATCH = "MISMATCH";
    public static final String CODE_MISSING_ACCOUNT_PAYABLE = "MISSING_ACCOUNT_PAYABLE";
    public static final String CODE_TOTAL_DRIFT = "TOTAL_DRIFT";
}

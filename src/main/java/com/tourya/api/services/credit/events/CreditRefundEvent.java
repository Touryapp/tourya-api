package com.tourya.api.services.credit.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TC-022 (#253): eventos de dominio del ciclo de vida de devolucion de credito.
 *
 * <p>Publicados desde {@link com.tourya.api.services.CreditService} y consumidos
 * por {@link CreditRefundEventListener} con
 * {@code @TransactionalEventListener(AFTER_COMMIT)}. Esto garantiza:</p>
 * <ul>
 *   <li>Si la tx principal hace rollback, el email nunca se envia.</li>
 *   <li>Si el envio del email falla, la tx principal ya cerro — no aborta la
 *       actualizacion de estado del credito (fire-and-forget).</li>
 * </ul>
 *
 * <p>Snapshot: los eventos llevan los datos ya resueltos (amount, timestamp,
 * proofUrl) para evitar re-queries en el listener y problemas de sesion JPA
 * cerrada. El userId se usa para resolver el destinatario del email en el
 * listener.</p>
 */
public sealed interface CreditRefundEvent {

    /**
     * Email 1: turista solicito la devolucion — status paso de CREATED a
     * REFUND_REQUESTED. Se envia confirmacion de recepcion.
     */
    record RefundRequested(
            Long creditId,
            Long reservationId,
            Integer userId,
            BigDecimal amount,
            LocalDateTime refundRequestedAt
    ) implements CreditRefundEvent {}

    /**
     * Email 2: ADMIN/BACKOFFICE subio el comprobante — status paso de
     * REFUND_REQUESTED a REFUNDED. Se envia confirmacion con link al
     * comprobante.
     */
    record RefundCompleted(
            Long creditId,
            Long reservationId,
            Integer userId,
            BigDecimal amount,
            LocalDateTime refundedAt,
            String proofUrl
    ) implements CreditRefundEvent {}
}

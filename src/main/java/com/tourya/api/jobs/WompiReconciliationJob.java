package com.tourya.api.jobs;

import com.tourya.api.models.Payment;
import com.tourya.api.models.WompiWebhookEvent;
import com.tourya.api.repository.PaymentRepository;
import com.tourya.api.repository.WompiWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Reconcilia eventos webhook Wompi (APPROVED con firma valida) contra Payments
 * existentes.
 * <ul>
 *   <li>Si Tourya ya tiene el Payment para ese transactionId (flujo normal turista):
 *       linkea el evento con el Payment (linked_payment_id) y lo marca procesado.
 *   <li>Si NO existe Payment (pago huerfano: Wompi aprobo pero el cliente nunca llamo
 *       POST /payment): loguea WARN y marca procesado para no retocarlo. Queda visible
 *       en la tabla wompi_webhook_event con linked_payment_id NULL para operacion manual.
 * </ul>
 * <p>Este job NO confirma reservas automaticamente porque el matching de un pago
 * huerfano con las reservas TEMPORAL correctas requiere conocer la reference que el
 * cliente uso al iniciar el checkout (no persistida hoy). Se atacara en un PR posterior
 * junto con el cambio de modelo que agregue wompi_reference a shopping_cart.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WompiReconciliationJob {

    private static final String STATUS_APPROVED = "APPROVED";

    private final WompiWebhookEventRepository eventRepository;
    private final PaymentRepository paymentRepository;

    @Scheduled(fixedDelayString = "${tourya.wompiReconciliation.fixedDelayMs:300000}")
    @Transactional
    public void reconcile() {
        List<WompiWebhookEvent> pending = eventRepository
                .findTop100BySignatureValidTrueAndProcessedAtIsNullAndTransactionStatusOrderByReceivedAtAsc(STATUS_APPROVED);

        if (pending.isEmpty()) {
            return;
        }

        int matched = 0;
        int orphan = 0;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (WompiWebhookEvent event : pending) {
            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(event.getTransactionId());
            if (paymentOpt.isPresent()) {
                event.setLinkedPaymentId(paymentOpt.get().getPaymentId());
                event.setProcessedAt(now);
                matched++;
            } else {
                log.warn("Wompi orphan payment detected: event_id={}, tx_id={}, reference={}, amount_cents={}, currency={}",
                        event.getId(),
                        event.getTransactionId(),
                        event.getTransactionReference(),
                        event.getAmountInCents(),
                        event.getCurrency());
                event.setProcessedAt(now);
                orphan++;
            }
        }
        eventRepository.saveAll(pending);

        log.info("WompiReconciliationJob completed: matched={}, orphan={} (total processed: {})",
                matched, orphan, pending.size());
    }
}

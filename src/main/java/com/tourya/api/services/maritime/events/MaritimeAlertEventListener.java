package com.tourya.api.services.maritime.events;

import com.tourya.api.services.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * BE-23: reacciona a {@link MaritimeAlertCreatedEvent} DESPUES del commit del create
 * del reporte. Delega a
 * {@link ReservationService#cancelAffectedByRedAlert(MaritimeAlertCreatedEvent)}.
 *
 * <p>Ninguna excepcion propaga fuera del listener — se logea WARN y termina, igual
 * patron que {@code PushDomainEventListener}.</p>
 *
 * <p>TC-018 (#227 3ra iter): removido {@code @Async}. Con async + AFTER_COMMIT
 * el listener nunca se disparaba en dev — el pod HTTP terminaba la request y el
 * hilo del executor no llegaba a correr o el evento no lo alcanzaba (no hay logs
 * BE-23 en Cloud Run cuando Luis creo el reporte 47). Sincronico corre en el mismo
 * thread del request → siempre se ejecuta post-commit. Trade-off: la request POST
 * /maritime-activity-reports tarda un poco mas pero es endpoint admin, no critico.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaritimeAlertEventListener {

    private final ReservationService reservationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRedAlert(MaritimeAlertCreatedEvent event) {
        log.info("BE-23 onRedAlert LISTENER FIRED for reportId={}", event.reportId());
        try {
            reservationService.cancelAffectedByRedAlert(event);
        } catch (Exception ex) {
            log.warn("BE-23 alert {} handler failed: {}", event.reportId(), ex.getMessage(), ex);
        }
    }
}

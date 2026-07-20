package com.tourya.api.services.maritime.events;

import com.tourya.api.services.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * BE-23: reacciona a {@link MaritimeAlertCreatedEvent} DESPUES del commit del create
 * del reporte, en un hilo aparte ({@code @Async}). Delega a
 * {@link ReservationService#cancelAffectedByRedAlert(MaritimeAlertCreatedEvent)}.
 *
 * <p>Ninguna excepcion propaga fuera del listener — se logea WARN y termina, igual
 * patron que {@code PushDomainEventListener}.</p>
 *
 * <p><b>Deuda BE-23b</b>: falta test integrado con Testcontainers que verifique
 * end-to-end la cadena create-report(RED) → listener → cancelaciones + creditos +
 * push events. Hoy solo hay unit test del listener y auditoria manual del service.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaritimeAlertEventListener {

    private final ReservationService reservationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRedAlert(MaritimeAlertCreatedEvent event) {
        try {
            reservationService.cancelAffectedByRedAlert(event);
        } catch (Exception ex) {
            log.warn("BE-23 alert {} handler failed: {}", event.reportId(), ex.getMessage());
        }
    }
}

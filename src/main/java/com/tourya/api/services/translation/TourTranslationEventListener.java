package com.tourya.api.services.translation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * IA-09: escucha {@link TourTranslationEvent} DESPUES del commit de la tx que
 * guarda el tour y delega la traduccion al metodo {@code @Async} de
 * {@link TourTranslationApplier}.
 *
 * <p>Sin {@code @Async} en el listener — el metodo publico de
 * {@link TourTranslationApplier#translateTourAsync} ya lo esta, asi que la
 * carga de calls a Google Translate se despacha al executor sin bloquear la
 * respuesta HTTP del save. Se evita el bug observado en
 * {@code MaritimeAlertEventListener} (async + AFTER_COMMIT no disparaba en
 * dev). Mismo patron que {@link com.tourya.api.services.credit.events.CreditRefundEventListener}.</p>
 *
 * <p>AFTER_COMMIT garantiza que:</p>
 * <ul>
 *   <li>Si la tx del save hace rollback, la traduccion no se dispara.</li>
 *   <li>Si la traduccion falla, el tour ya quedo persistido en espanol —
 *       {@link com.tourya.api.models.TranslatedField#get(String)} hace fallback
 *       automatico a ES para en/pt vacios.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TourTranslationEventListener {

    private final TourTranslationApplier applier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TourTranslationEvent event) {
        if (event == null || event.tourId() == null) {
            log.debug("IA-09 TourTranslationEvent without tourId — ignored");
            return;
        }
        log.debug("IA-09 TourTranslationEvent received tourId={} — dispatching async", event.tourId());
        applier.translateTourAsync(event.tourId());
    }
}

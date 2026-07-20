package com.tourya.api.services.push;

import com.tourya.api.services.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * MO-40b: listener que reacciona a {@link PushDomainEvent} DESPUES de que la
 * tx de negocio hizo commit y en un hilo aparte ({@code @Async}). Si el push
 * falla no afecta al productor ni a la tx original.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDomainEventListener {

    private final PushNotificationService pushService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PushDomainEvent.ReservationConfirmedForTourist e) {
        safeSend("ReservationConfirmedForTourist", e.reservationId(),
                () -> pushService.notifyReservationConfirmedForTourist(
                        e.touristUserId(), e.tourName(), e.reservationId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PushDomainEvent.NewReservationForProvider e) {
        safeSend("NewReservationForProvider", e.reservationId(),
                () -> pushService.notifyNewReservationForProvider(
                        e.providerUserId(), e.tourName(), e.reservationId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PushDomainEvent.ReviewReplied e) {
        safeSend("ReviewReplied", e.reservationId(),
                () -> pushService.notifyReviewRepliedForTourist(
                        e.touristUserId(), e.tourName(), e.reservationId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PushDomainEvent.TourReminder24h e) {
        safeSend("TourReminder24h", e.reservationId(),
                () -> pushService.notifyTourReminderForTourist(
                        e.touristUserId(), e.tourName(), e.reservationId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PushDomainEvent.CreditExpiringSoon e) {
        safeSend("CreditExpiringSoon", null,
                () -> pushService.notifyCreditExpiringSoonForTourist(
                        e.userId(), e.daysAdvance()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PushDomainEvent.CreditExpired e) {
        safeSend("CreditExpired", null,
                () -> pushService.notifyCreditExpiredForTourist(e.userId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PushDomainEvent.ReservationCanceledByRain e) {
        safeSend("ReservationCanceledByRain", e.reservationId(),
                () -> pushService.notifyReservationCanceledByRainForTourist(
                        e.touristUserId(), e.tourName(), e.reservationId()));
    }

    private void safeSend(String eventType, Long correlationId, Runnable send) {
        try {
            send.run();
        } catch (Exception ex) {
            log.warn("MO-40b push failed type={} correlationId={} msg={}",
                    eventType, correlationId, ex.getMessage());
        }
    }
}

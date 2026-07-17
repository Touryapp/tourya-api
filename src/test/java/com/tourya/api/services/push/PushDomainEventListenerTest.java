package com.tourya.api.services.push;

import com.tourya.api.services.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * MO-40b — cubre el fan-out del listener a cada helper de {@link PushNotificationService}
 * mas invariantes: cada handler es {@code @Async} + {@code @TransactionalEventListener(AFTER_COMMIT)}
 * y ninguna excepcion del push propaga fuera del listener (safeSend).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MO-40b PushDomainEventListener")
class PushDomainEventListenerTest {

    @Mock private PushNotificationService pushService;

    private PushDomainEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PushDomainEventListener(pushService);
    }

    @Test
    @DisplayName("ReservationConfirmedForTourist -> notifyReservationConfirmedForTourist")
    void reservationConfirmed_fanOut() {
        listener.on(new PushDomainEvent.ReservationConfirmedForTourist(10, "Rafting", 400L));
        verify(pushService).notifyReservationConfirmedForTourist(10, "Rafting", 400L);
    }

    @Test
    @DisplayName("NewReservationForProvider -> notifyNewReservationForProvider")
    void newReservationForProvider_fanOut() {
        listener.on(new PushDomainEvent.NewReservationForProvider(20, "Kayak", 401L));
        verify(pushService).notifyNewReservationForProvider(20, "Kayak", 401L);
    }

    @Test
    @DisplayName("ReviewReplied -> notifyReviewRepliedForTourist")
    void reviewReplied_fanOut() {
        listener.on(new PushDomainEvent.ReviewReplied(30, "Snorkel", 402L));
        verify(pushService).notifyReviewRepliedForTourist(30, "Snorkel", 402L);
    }

    @Test
    @DisplayName("TourReminder24h -> notifyTourReminderForTourist")
    void tourReminder_fanOut() {
        listener.on(new PushDomainEvent.TourReminder24h(40, "Zipline", 403L));
        verify(pushService).notifyTourReminderForTourist(40, "Zipline", 403L);
    }

    @Test
    @DisplayName("CreditExpiringSoon -> notifyCreditExpiringSoonForTourist")
    void creditExpiring_fanOut() {
        listener.on(new PushDomainEvent.CreditExpiringSoon(50, 7));
        verify(pushService).notifyCreditExpiringSoonForTourist(50, 7);
    }

    @Test
    @DisplayName("CreditExpired -> notifyCreditExpiredForTourist")
    void creditExpired_fanOut() {
        listener.on(new PushDomainEvent.CreditExpired(60));
        verify(pushService).notifyCreditExpiredForTourist(60);
    }

    @Test
    @DisplayName("safeSend: excepcion en push NO propaga fuera del listener")
    void pushFailure_isSwallowed() {
        doThrow(new RuntimeException("FCM down"))
                .when(pushService).notifyReservationConfirmedForTourist(10, null, 999L);

        // No debe lanzar — safeSend traga el error y solo loguea.
        listener.on(new PushDomainEvent.ReservationConfirmedForTourist(10, null, 999L));

        verify(pushService).notifyReservationConfirmedForTourist(10, null, 999L);
    }

    @Test
    @DisplayName("invariante: todos los handlers son @Async + @TransactionalEventListener(AFTER_COMMIT)")
    void allHandlers_areAsyncAndAfterCommit() throws NoSuchMethodException {
        // Este test previene una regresion futura: si alguien quita @Async o cambia
        // la fase del TransactionalEventListener, este test lo detecta.
        for (Class<?> eventType : new Class<?>[] {
                PushDomainEvent.ReservationConfirmedForTourist.class,
                PushDomainEvent.NewReservationForProvider.class,
                PushDomainEvent.ReviewReplied.class,
                PushDomainEvent.TourReminder24h.class,
                PushDomainEvent.CreditExpiringSoon.class,
                PushDomainEvent.CreditExpired.class,
        }) {
            Method handler = PushDomainEventListener.class.getDeclaredMethod("on", eventType);
            Async async = AnnotatedElementUtils.findMergedAnnotation(handler, Async.class);
            TransactionalEventListener tel =
                    AnnotatedElementUtils.findMergedAnnotation(handler, TransactionalEventListener.class);

            assertThat(async).as("@Async en handler " + eventType.getSimpleName()).isNotNull();
            assertThat(tel).as("@TransactionalEventListener en handler " + eventType.getSimpleName()).isNotNull();
            assertThat(tel.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        }
    }
}

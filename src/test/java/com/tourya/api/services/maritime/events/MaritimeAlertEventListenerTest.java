package com.tourya.api.services.maritime.events;

import com.tourya.api.constans.enums.MaritimeFlagEnum;
import com.tourya.api.services.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * BE-23: cubre que el listener delegue al service y que ninguna excepcion se propague
 * fuera. La logica de cancelacion en si esta cubierta en {@link com.tourya.api.services.ReservationServiceRedAlertTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BE-23 MaritimeAlertEventListener")
class MaritimeAlertEventListenerTest {

    @Mock private ReservationService reservationService;

    @InjectMocks private MaritimeAlertEventListener listener;

    private static MaritimeAlertCreatedEvent sampleEvent() {
        return new MaritimeAlertCreatedEvent(
                42L,
                "paseo_al_cayo",
                1, 10, 100,
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 21),
                MaritimeFlagEnum.RED);
    }

    @Test
    @DisplayName("delega la cancelacion al ReservationService")
    void onRedAlert_delegates() {
        MaritimeAlertCreatedEvent event = sampleEvent();

        listener.onRedAlert(event);

        verify(reservationService).cancelAffectedByRedAlert(event);
    }

    @Test
    @DisplayName("excepcion del service NO propaga fuera del listener")
    void onRedAlert_swallowsServiceFailure() {
        MaritimeAlertCreatedEvent event = sampleEvent();
        doThrow(new RuntimeException("simulated DB error"))
                .when(reservationService).cancelAffectedByRedAlert(event);

        // No debe lanzar
        listener.onRedAlert(event);

        verify(reservationService).cancelAffectedByRedAlert(event);
    }
}

package com.tourya.api.jobs;

import com.tourya.api.models.Reservation;
import com.tourya.api.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Desactiva {@code canCancel} / {@code canReschedule} cuando vencen las ventanas de la reserva.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCancellationFlagsJob {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final ReservationRepository reservationRepository;

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Bogota")
    @Transactional
    public void expireCancellationAndRescheduleFlags() {
        LocalDate today = LocalDate.now(BOGOTA);
        List<Reservation> candidates = reservationRepository.findWithExpiredCancellationOrRescheduleFlags(today);
        if (candidates.isEmpty()) {
            return;
        }
        List<Reservation> toSave = new ArrayList<>();
        for (Reservation reservation : candidates) {
            boolean changed = false;
            if (Boolean.TRUE.equals(reservation.getCanCancel())
                    && reservation.getMaxCancellationDate() != null
                    && reservation.getMaxCancellationDate().isBefore(today)) {
                reservation.setCanCancel(false);
                changed = true;
            }
            if (Boolean.TRUE.equals(reservation.getCanReschedule())
                    && reservation.getMaxReschedulingDate() != null
                    && reservation.getMaxReschedulingDate().isBefore(today)) {
                reservation.setCanReschedule(false);
                changed = true;
            }
            if (changed) {
                toSave.add(reservation);
            }
        }
        if (!toSave.isEmpty()) {
            reservationRepository.saveAll(toSave);
            log.info("ReservationCancellationFlagsJob: updated {} reservation(s) for date {}", toSave.size(), today);
        }
    }
}

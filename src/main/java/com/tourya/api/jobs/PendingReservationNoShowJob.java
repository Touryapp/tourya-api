package com.tourya.api.jobs;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.models.Reservation;
import com.tourya.api.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingReservationNoShowJob {

    private static final ZoneId CO_ZONE = ZoneId.of("America/Bogota");

    private final ReservationRepository reservationRepository;

    /**
     * Diario 7am (hora Colombia): marca como NO_SHOW las reservas PENDING cuyo scheduleDate ya pasó.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "America/Bogota")
    @Transactional
    public void markNoShows() {
        LocalDate today = LocalDate.now(CO_ZONE);
        List<Reservation> pendingPast = reservationRepository.findPendingWithScheduleDateBefore(
                DeliveryStatusEnum.PENDING,
                today
        );
        if (pendingPast.isEmpty()) return;

        for (Reservation r : pendingPast) {
            try {
                r.setDeliveryStatus(DeliveryStatusEnum.NO_SHOW);
                reservationRepository.save(r);
            } catch (Exception e) {
                log.warn("No se pudo marcar NO_SHOW la reserva {}: {}", r.getReservationId(), e.getMessage());
            }
        }

        log.info("Marcadas {} reservas como NO_SHOW (scheduleDate < {})", pendingPast.size(), today);
    }
}


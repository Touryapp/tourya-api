package com.tourya.api.jobs;

import com.tourya.api.models.Reservation;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourSchedule;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.services.push.PushDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * MO-40 Fase D: recordatorio 24 horas antes del tour para el turista.
 *
 * <p>Corre a las 8:00 AM Bogota. Busca todas las reservas
 * {@code deliveryStatus = PENDING} cuya fecha de tour es <b>mañana</b>, y
 * envia push al turista con deep-link a la pantalla de reservacion (para que
 * tenga el QR a mano).</p>
 *
 * <p><b>Idempotencia:</b> el job no marca un timestamp por reserva — si corre
 * varias veces el mismo dia, mandara varios push. Como el cron es fixed a
 * 8:00 AM diaria y el {@code @Scheduled} de Spring garantiza que no se
 * ejecuta dos veces concurrentes en la misma instancia, en operacion normal
 * se envia exactamente 1 vez por reserva. Si Cloud Run corre con multiples
 * instancias en el futuro, se puede agregar coordinacion via un lock en BD
 * o mover a Cloud Scheduler + Pub/Sub.</p>
 *
 * <p>Errores por reserva individual no interrumpen el resto — se loguean.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TourReminder24hJob {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher; // MO-40b

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Bogota")
    @Transactional(readOnly = true)
    public void runDaily() {
        LocalDate tomorrow = LocalDate.now(BOGOTA).plusDays(1);
        List<Reservation> reservations = reservationRepository.findPendingForTourDate(tomorrow);

        if (reservations.isEmpty()) {
            log.info("TourReminder24hJob: no reservations for tomorrow ({})", tomorrow);
            return;
        }

        int sent = 0;
        for (Reservation reservation : reservations) {
            try {
                ShoppingCartItem item = reservation.getShoppingCartItem();
                if (item == null || item.getShoppingCart() == null || item.getShoppingCart().getUser() == null) {
                    log.warn("TourReminder24hJob: reservation {} lacks cart/user linkage", reservation.getReservationId());
                    continue;
                }
                Integer touristUserId = item.getShoppingCart().getUser().getId();

                TourSchedule schedule = item.getTourSchedule();
                Tour tour = schedule != null ? schedule.getTour() : null;
                String tourName = tour != null && tour.getName() != null ? tour.getName().getEs() : null;

                eventPublisher.publishEvent(new PushDomainEvent.TourReminder24h(
                        touristUserId, tourName, reservation.getReservationId()));
                sent++;
            } catch (Exception ex) {
                log.warn("TourReminder24hJob: failed for reservation {}: {}",
                        reservation.getReservationId(), ex.getMessage());
            }
        }
        log.info("TourReminder24hJob: sent={} of {} reservations for tomorrow ({})",
                sent, reservations.size(), tomorrow);
    }
}

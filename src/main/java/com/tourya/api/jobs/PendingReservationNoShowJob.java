package com.tourya.api.jobs;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.services.TourScheduleSlotAvailabilityService;
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
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final TourScheduleSlotAvailabilityService tourScheduleSlotAvailabilityService;

    /**
     * Diario 7am (hora Colombia): marca como NO_SHOW las reservas PENDING cuyo scheduleDate ya pasó.
     * BE-27: por cada NO_SHOW llama {@code recalculate(slotId)} para que {@code slot.bookings}
     * refleje la liberacion del cupo (antes quedaba inflado con el drift acumulandose).
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

        java.util.Set<Integer> slotIdsToRecalc = new java.util.HashSet<>();
        for (Reservation r : pendingPast) {
            try {
                r.setDeliveryStatus(DeliveryStatusEnum.NO_SHOW);
                reservationRepository.save(r);
                // BE-27: acumular slotIds afectados para recalcular al final (una vez por slot).
                if (r.getItemId() != null) {
                    ShoppingCartItem item = shoppingCartItemRepository.findById(r.getItemId()).orElse(null);
                    if (item != null && item.getSlot() != null && item.getSlot().getId() != null) {
                        slotIdsToRecalc.add(item.getSlot().getId());
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo marcar NO_SHOW la reserva {}: {}", r.getReservationId(), e.getMessage());
            }
        }

        // BE-27: recalcular disponibilidad de cada slot afectado una sola vez.
        int recalcOk = 0;
        int recalcFailed = 0;
        for (Integer slotId : slotIdsToRecalc) {
            try {
                tourScheduleSlotAvailabilityService.recalculate(slotId);
                recalcOk++;
            } catch (Exception e) {
                recalcFailed++;
                log.warn("BE-27: recalculate fallo para slot {} tras marcar NO_SHOW: {}", slotId, e.getMessage());
            }
        }

        log.info("Marcadas {} reservas como NO_SHOW (scheduleDate < {}). Recalculados {} slots (fallidos: {}).",
                pendingPast.size(), today, recalcOk, recalcFailed);
    }
}


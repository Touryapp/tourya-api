package com.tourya.api.jobs;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemporalReservationExpiryJob {

    private final ReservationRepository reservationRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;

    /**
     * Expira holds temporales vencidos.
     * Nota: liberación de disponibilidad se recalcula al próximo hold/payment/cancel; aquí solo marca CANCELED.
     */
    @Scheduled(fixedDelayString = "${tourya.temporalReservationExpiry.fixedDelayMs:60000}")
    @Transactional
    public void expireTemporalReservations() {
        // Mantener coherencia con creación del hold (UTC) para evitar expiraciones inmediatas por zona horaria
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("UTC"));
        List<Reservation> expired = reservationRepository.findExpiredTemporalReservations(now);
        if (expired.isEmpty()) return;

        for (Reservation r : expired) {
            try {
                r.setDeliveryStatus(DeliveryStatusEnum.CANCELED);
                r.setCancellationDate(now);
                r.setExpiresAt(null);
                reservationRepository.save(r);

                ShoppingCartItem item = shoppingCartItemRepository.findById(r.getItemId()).orElse(null);
                if (item != null) {
                    // mantener el item en el carrito pero quitar la reserva temporal para permitir reintento
                    item.setReservationId(null);
                    shoppingCartItemRepository.save(item);
                }
            } catch (Exception e) {
                log.warn("No se pudo expirar reserva temporal {}: {}", r.getReservationId(), e.getMessage());
            }
        }

        log.info("Expiradas {} reservas temporales vencidas", expired.size());
    }
}


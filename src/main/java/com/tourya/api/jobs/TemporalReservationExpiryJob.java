package com.tourya.api.jobs;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.Credit;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.repository.CreditRepository;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.services.TourScheduleSlotAvailabilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class TemporalReservationExpiryJob {

    private final ReservationRepository reservationRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final CreditRepository creditRepository;
    private final TourScheduleSlotAvailabilityService tourScheduleSlotAvailabilityService;
    private final TransactionTemplate perReservationTx;

    public TemporalReservationExpiryJob(ReservationRepository reservationRepository,
                                        ShoppingCartItemRepository shoppingCartItemRepository,
                                        CreditRepository creditRepository,
                                        TourScheduleSlotAvailabilityService tourScheduleSlotAvailabilityService,
                                        PlatformTransactionManager transactionManager) {
        this.reservationRepository = reservationRepository;
        this.shoppingCartItemRepository = shoppingCartItemRepository;
        this.creditRepository = creditRepository;
        this.tourScheduleSlotAvailabilityService = tourScheduleSlotAvailabilityService;
        // Hotfix #179b: cada reserva se procesa en su propia tx REQUIRES_NEW para
        // que un fallo aislado no marque rollback-only la tx del batch y arrastre
        // al resto (el job entraba en loop de UnexpectedRollbackException).
        this.perReservationTx = new TransactionTemplate(transactionManager);
        this.perReservationTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Expira holds temporales vencidos. Cada reserva corre en su propia transaccion.
     * BE-27: al final de {@link #expireOne} se llama {@code recalculate(slotId)} para que
     * {@code slot.bookings/availability} reflejen la liberacion del cupo (antes se dejaba
     * inflado hasta el proximo hold, causando drift acumulado sobre el tiempo).
     */
    @Scheduled(fixedDelayString = "${tourya.temporalReservationExpiry.fixedDelayMs:60000}")
    public void expireTemporalReservations() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("UTC"));
        List<Reservation> expired = reservationRepository.findExpiredTemporalReservations(now);
        if (expired.isEmpty()) return;

        int ok = 0;
        int failed = 0;
        for (Reservation r : expired) {
            try {
                perReservationTx.executeWithoutResult(status -> expireOne(r, now));
                ok++;
            } catch (Exception e) {
                failed++;
                log.warn("No se pudo expirar reserva temporal {}: {}", r.getReservationId(), e.getMessage());
            }
        }

        log.info("Expiradas {} reservas temporales vencidas (fallidas aisladas: {})", ok, failed);
    }

    private void expireOne(Reservation r, LocalDateTime now) {
        r.setDeliveryStatus(DeliveryStatusEnum.CANCELED);
        r.setCancellationDate(now);
        r.setExpiresAt(null);
        reservationRepository.save(r);

        Long itemId = r.getItemId();
        if (itemId == null) {
            // Hotfix #179b: reservas con item_id NULL vienen del FK ON DELETE SET NULL
            // (migracion 012) cuando el shopping_cart_item asociado se borro por reschedule
            // u otro flujo. Solo hay que marcar CANCELED — no hay item ni creditos que limpiar.
            return;
        }

        ShoppingCartItem item = shoppingCartItemRepository.findById(itemId).orElse(null);
        Integer slotIdForRecalc = null;
        if (item != null) {
            if (item.getSlot() != null && item.getSlot().getId() != null) {
                slotIdForRecalc = item.getSlot().getId();
            }
            // mantener el item en el carrito pero quitar la reserva temporal para permitir reintento
            item.setReservationId(null);
            shoppingCartItemRepository.save(item);
        }

        // Liberar creditos reservados para este item si el pago no se completo.
        // Regla: si Credit esta RESERVED y asociado al shopping_cart_item_id, lo devolvemos a CREATED.
        java.util.Set<Long> itemIds = java.util.Set.of(itemId);
        List<Credit> reservedCredits = creditRepository.findByShoppingCartItemIdInAndStatusReserved(itemIds);
        for (Credit c : reservedCredits) {
            c.setReservedAmount(java.math.BigDecimal.ZERO);
            c.setShoppingCartItemId(null);
            c.setStatus(CreditStatusEnum.CREATED);
        }
        if (!reservedCredits.isEmpty()) {
            creditRepository.saveAll(reservedCredits);
        }

        // BE-27: recalcular slot.bookings/availability para que reflejen que este hold libero cupo.
        if (slotIdForRecalc != null) {
            try {
                tourScheduleSlotAvailabilityService.recalculate(slotIdForRecalc);
            } catch (Exception e) {
                // No propagar: el hold ya se cancelo. El drift se corrige al proximo touch del slot.
                log.warn("BE-27: recalculate fallo para slot {} tras expirar reserva {}: {}",
                        slotIdForRecalc, r.getReservationId(), e.getMessage());
            }
        }
    }
}

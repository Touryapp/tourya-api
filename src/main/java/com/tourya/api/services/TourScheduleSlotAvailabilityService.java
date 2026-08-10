package com.tourya.api.services;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.TourScheduleConfigRepository;
import com.tourya.api.repository.TourScheduleConfigSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TC-004: bookings/availability se calculan por (slot_id, schedule_date) en runtime.
 *
 * Las columnas denormalizadas {@code tour_schedule_config_slot.bookings} y {@code availability}
 * quedan preservadas por compatibilidad pero ya no son la fuente de verdad — se contaba a nivel
 * de config-slot y esa cifra se replicaba en todas las fechas del calendario mensual (bug reportado
 * en el issue TC-004 el 2026-07-21).
 *
 * Nueva fuente de verdad: {@link ReservationRepository#countActiveBookingUnitsForSlotOnDate}.
 */
@Service
@RequiredArgsConstructor
public class TourScheduleSlotAvailabilityService {

    private final TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ReservationRepository reservationRepository;
    private final TourRepository tourRepository;
    private final TourScheduleConfigRepository tourScheduleConfigRepository;

    /**
     * {@code min_capacity_calc} = 40% de {@code capacity} (floor). Solo si el tour es limitado.
     * {@code check_availability} = true si (grupo y min &lt; 5) o (individual y min &lt; 20).
     */
    public void applyMinCapacityAndCheckAvailability(TourScheduleConfigSlot slot, Tour tour) {
        if (tour == null || Boolean.TRUE.equals(tour.getIsUnlimitedCapacity()) || slot.getCapacity() == null) {
            slot.setMinCapacityCalc(null);
            slot.setCheckAvailability(false);
            return;
        }
        int minCap = (int) Math.floor(slot.getCapacity() * 0.4);
        slot.setMinCapacityCalc(minCap);
        boolean isGrupo = tour.getPriceType() != null && "grupo".equalsIgnoreCase(tour.getPriceType().getValue());
        slot.setCheckAvailability((isGrupo && minCap < 5) || (!isGrupo && minCap < 20));
    }

    public int bookingUnitsForCartItem(Tour tour, ShoppingCartItem item) {
        if (tour.getPriceType() != null && "grupo".equalsIgnoreCase(tour.getPriceType().getValue())) {
            return 1;
        }
        if (item.getDetails() == null) {
            return 0;
        }
        return item.getDetails().stream().mapToInt(ShoppingCartItemDetail::getQuantity).sum();
    }

    /**
     * TC-004: cuenta unidades reservadas para un slot en una fecha específica
     * respetando priceType (grupo=1 unidad por reserva, individual=suma de pax).
     *
     * @param slotId       id del {@link TourScheduleConfigSlot}
     * @param scheduleDate fecha del schedule (no la del recalculate del slot config)
     * @return unidades reservadas activas (TEMPORAL/PENDING/DELIVERED), 0 si no hay ninguna
     */
    public int countBookingsForSlotOnDate(Integer slotId, LocalDate scheduleDate) {
        if (slotId == null || scheduleDate == null) {
            return 0;
        }
        Integer count = reservationRepository.countActiveBookingUnitsForSlotOnDate(slotId, scheduleDate);
        return count != null ? count : 0;
    }

    /**
     * Valida cupos en el slot para una fecha específica (TC-004).
     *
     * @param scheduleDate fecha real del schedule que se está reservando; obligatoria para
     *                     evitar el bug del bookings global (ver TC-004).
     */
    public void ensureSlotHasCapacity(Tour tour, TourScheduleConfigSlot slot, LocalDate scheduleDate, int participantTotal) {
        if (Boolean.TRUE.equals(tour.getIsUnlimitedCapacity())) {
            return;
        }
        if (slot.getCapacity() == null) {
            throw new OperationNotPermittedException("El slot no tiene capacity configurada");
        }
        boolean isGrupo = tour.getPriceType() != null && "grupo".equalsIgnoreCase(tour.getPriceType().getValue());
        if (isGrupo && tour.getMaxPeople() != null && participantTotal > tour.getMaxPeople()) {
            throw new OperationNotPermittedException(
                    "El grupo supera el máximo de personas por reserva del tour (" + tour.getMaxPeople() + ")");
        }
        int slotUnits = isGrupo ? 1 : participantTotal;
        int booked = countBookingsForSlotOnDate(slot.getId(), scheduleDate);
        int available = Math.max(0, slot.getCapacity() - booked);
        if (slotUnits > available) {
            String unitLabel = isGrupo ? "cupos de grupo" : "plazas";
            throw new OperationNotPermittedException(
                    "No hay disponibilidad suficiente en el turno (slot). Disponible: " + available + " " + unitLabel);
        }
    }

    /**
     * TC-011 (#206 reabierto Luis 2026-08-04): `REQUIRES_NEW` para aislar la
     * transaccion del caller. Sin esto, si `Slot not found` (u otro runtime
     * exception aqui) marcaba la transaccion externa del `PendingReservationNoShowJob`
     * como rollback-only, revirtiendo TODOS los `save(reservation)` del job
     * (log: `UnexpectedRollbackException: silently rolled back`). Efecto visible:
     * el job decia "Marcadas 11 reservas" pero al hacer commit revertia todo.
     *
     * TC-019 (#231): usar `recalculate` desde flujos donde el slot YA esta commiteado
     * en BD (jobs, cancelaciones post-reserva). Para llamadas desde flujos donde el
     * slot recien se creo/actualizo en la MISMA tx externa (batch de tour-schedule),
     * usar `recalculateInSameTransaction` — sino REQUIRES_NEW no ve los INSERTs
     * pendientes y lanza `ResourceNotFoundException("Slot not found")` -> 404.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculate(Integer slotId) {
        doRecalculate(slotId);
    }

    /**
     * TC-019 (#231): variante en la MISMA transaccion del caller. Usada por el batch
     * de tour-schedule (create/update config) donde los slots recien fueron persistidos
     * pero aun no commiteados. Si falla, marca la tx externa rollback-only — que es
     * el comportamiento correcto: un slot faltante en el flujo de creacion es un bug real.
     */
    @Transactional
    public void recalculateInSameTransaction(Integer slotId) {
        doRecalculate(slotId);
    }

    private void doRecalculate(Integer slotId) {
        TourScheduleConfigSlot slot = tourScheduleConfigSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        List<ShoppingCartItem> itemsWithSlot = shoppingCartItemRepository.findAll().stream()
                .filter(i -> i.getSlot() != null && i.getSlot().getId() != null && i.getSlot().getId().equals(slotId))
                .collect(Collectors.toList());

        Set<Long> reservationIds = itemsWithSlot.stream()
                .map(ShoppingCartItem::getReservationId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        int bookings = 0;
        if (!reservationIds.isEmpty()) {
            List<Reservation> reservations = reservationRepository.findAllByReservationIdIn(new ArrayList<>(reservationIds));
            for (Reservation r : reservations) {
                if (r.getDeliveryStatus() == DeliveryStatusEnum.CANCELED
                        || r.getDeliveryStatus() == DeliveryStatusEnum.NO_SHOW) {
                    continue;
                }
                if (r.getDeliveryStatus() != DeliveryStatusEnum.TEMPORAL
                        && r.getDeliveryStatus() != DeliveryStatusEnum.PENDING
                        && r.getDeliveryStatus() != DeliveryStatusEnum.DELIVERED) {
                    continue;
                }
                ShoppingCartItem item = shoppingCartItemRepository.findById(r.getItemId()).orElse(null);
                if (item == null || item.getTourSchedule() == null || item.getTourSchedule().getTourId() == null) {
                    continue;
                }
                Tour tour = tourRepository.findById(item.getTourSchedule().getTourId()).orElse(null);
                if (tour == null) {
                    continue;
                }
                bookings += bookingUnitsForCartItem(tour, item);
            }
        }

        slot.setBookings(bookings);
        if (slot.getCapacity() != null) {
            slot.setAvailability(Math.max(0, slot.getCapacity() - bookings));
        } else {
            slot.setAvailability(0);
        }

        Tour refTour = resolveTourForSlot(slot, itemsWithSlot);
        applyMinCapacityAndCheckAvailability(slot, refTour);

        tourScheduleConfigSlotRepository.save(slot);
    }

    private Tour resolveTourForSlot(TourScheduleConfigSlot slot, List<ShoppingCartItem> itemsWithSlot) {
        for (ShoppingCartItem i : itemsWithSlot) {
            if (i.getTourSchedule() != null && i.getTourSchedule().getTourId() != null) {
                Tour t = tourRepository.findById(i.getTourSchedule().getTourId()).orElse(null);
                if (t != null) {
                    return t;
                }
            }
        }
        Integer configId = slot.getConfigId();
        if (configId == null && slot.getConfig() != null) {
            configId = slot.getConfig().getId();
        }
        if (configId != null) {
            TourScheduleConfig cfg = tourScheduleConfigRepository.findById(configId).orElse(null);
            if (cfg != null && cfg.getTourId() != null) {
                return tourRepository.findById(cfg.getTourId()).orElse(null);
            }
        }
        return null;
    }
}

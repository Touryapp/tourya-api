package com.tourya.api.services;

import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.ReservationStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.TourReservationDetailRepository;
import com.tourya.api.repository.TourScheduleConfigRepository;
import com.tourya.api.repository.TourScheduleConfigSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bookings y availability viven en {@link TourScheduleConfigSlot}.
 * Bookings = suma de unidades de reserva (TEMPORAL/PENDING/DELIVERED, sin canceladas) por slot.
 */
@Service
@RequiredArgsConstructor
public class TourScheduleSlotAvailabilityService {

    private final TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ReservationRepository reservationRepository;
    private final TourRepository tourRepository;
    private final TourReservationDetailRepository tourReservationDetailRepository;
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
     * Valida cupos en el slot usando capacity − bookings (no solo la columna availability).
     */
    public void ensureSlotHasCapacity(Tour tour, TourScheduleConfigSlot slot, int participantTotal) {
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
        int booked = slot.getBookings() != null ? slot.getBookings() : 0;
        int available = Math.max(0, slot.getCapacity() - booked);
        if (slotUnits > available) {
            String unitLabel = isGrupo ? "cupos de grupo" : "plazas";
            throw new OperationNotPermittedException(
                    "No hay disponibilidad suficiente en el turno (slot). Disponible: " + available + " " + unitLabel);
        }
    }

    @Transactional
    public void recalculate(Integer slotId) {
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
                if (r.getDeliveryStatus() == DeliveryStatusEnum.CANCELED) {
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

        bookings += legacyTourReservationUnitsForSlot(slotId);

        slot.setBookings(bookings);
        if (slot.getCapacity() != null) {
            slot.setAvailability(Math.max(0, slot.getCapacity() - bookings));
        } else {
            slot.setAvailability(0);
        }

        Tour refTour = resolveTourForSlot(slotId, slot, itemsWithSlot);
        applyMinCapacityAndCheckAvailability(slot, refTour);

        tourScheduleConfigSlotRepository.save(slot);
    }

    /**
     * Reservas legacy ({@link com.tourya.api.models.TourReservation}) en PENDING que usan este slot.
     */
    private int legacyTourReservationUnitsForSlot(Integer slotId) {
        List<TourReservationDetail> details = tourReservationDetailRepository.findByPrice_Slot_Id(slotId);
        if (details.isEmpty()) {
            return 0;
        }
        Map<Integer, List<TourReservationDetail>> byReservation = details.stream()
                .filter(d -> d.getReservation() != null
                        && d.getReservation().getStatus() != ReservationStatusEnum.CANCELED
                        && d.getReservation().getStatus() != ReservationStatusEnum.COMPLETED)
                .collect(Collectors.groupingBy(d -> d.getReservation().getId()));
        int sum = 0;
        for (List<TourReservationDetail> group : byReservation.values()) {
            com.tourya.api.models.TourReservation res = group.get(0).getReservation();
            if (res.getSchedule() == null || res.getSchedule().getTourId() == null) {
                continue;
            }
            Tour tour = tourRepository.findById(res.getSchedule().getTourId()).orElse(null);
            if (tour == null) {
                continue;
            }
            int qtyOnSlot = group.stream().mapToInt(TourReservationDetail::getQuantity).sum();
            if (tour.getPriceType() != null && "grupo".equalsIgnoreCase(tour.getPriceType().getValue())) {
                sum += 1;
            } else {
                sum += qtyOnSlot;
            }
        }
        return sum;
    }

    /**
     * Tour de referencia para reglas de min capacity: carrito → config del slot → reserva legacy.
     */
    private Tour resolveTourForSlot(Integer slotId, TourScheduleConfigSlot slot, List<ShoppingCartItem> itemsWithSlot) {
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
                Tour t = tourRepository.findById(cfg.getTourId()).orElse(null);
                if (t != null) {
                    return t;
                }
            }
        }
        List<TourReservationDetail> details = tourReservationDetailRepository.findByPrice_Slot_Id(slotId);
        for (TourReservationDetail d : details) {
            if (d.getReservation() == null || d.getReservation().getSchedule() == null) {
                continue;
            }
            Integer tid = d.getReservation().getSchedule().getTourId();
            if (tid != null) {
                Tour t = tourRepository.findById(tid).orElse(null);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }
}

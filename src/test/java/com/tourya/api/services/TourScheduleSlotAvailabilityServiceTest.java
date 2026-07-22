package com.tourya.api.services;

import com.tourya.api.constans.enums.PriceTypeEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.TourScheduleConfigRepository;
import com.tourya.api.repository.TourScheduleConfigSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * TC-004: bookings/availability se cuentan por (slot_id, schedule_date) en runtime,
 * NO desde el campo denormalizado `slot.bookings`.
 *
 * Estos tests protegen el fix del bug reportado por Luis 2026-07-21 donde el mismo
 * `slot.bookings` aparecía en TODAS las fechas del calendario mensual del tour.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TourScheduleSlotAvailabilityService — TC-004 bookings por día")
class TourScheduleSlotAvailabilityServiceTest {

    private static final LocalDate DATE_24_JUL = LocalDate.of(2026, 7, 24);
    private static final LocalDate DATE_25_JUL = LocalDate.of(2026, 7, 25);
    private static final Integer SLOT_ID = 1030;

    @Mock private TourScheduleConfigSlotRepository slotRepository;
    @Mock private ShoppingCartItemRepository shoppingCartItemRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private TourRepository tourRepository;
    @Mock private TourScheduleConfigRepository configRepository;

    private TourScheduleSlotAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new TourScheduleSlotAvailabilityService(
                slotRepository, shoppingCartItemRepository, reservationRepository,
                tourRepository, configRepository);
    }

    // ---------------- countBookingsForSlotOnDate ----------------

    @Test
    @DisplayName("countBookingsForSlotOnDate: devuelve 0 si slotId es null")
    void countBookings_nullSlot_returnsZero() {
        int result = service.countBookingsForSlotOnDate(null, DATE_24_JUL);

        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository);
    }

    @Test
    @DisplayName("countBookingsForSlotOnDate: devuelve 0 si scheduleDate es null")
    void countBookings_nullDate_returnsZero() {
        int result = service.countBookingsForSlotOnDate(SLOT_ID, null);

        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository);
    }

    @Test
    @DisplayName("countBookingsForSlotOnDate: devuelve valor del repository")
    void countBookings_happyPath_returnsRepositoryValue() {
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_24_JUL))
                .thenReturn(2);

        int result = service.countBookingsForSlotOnDate(SLOT_ID, DATE_24_JUL);

        assertThat(result).isEqualTo(2);
        verify(reservationRepository).countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_24_JUL);
    }

    @Test
    @DisplayName("countBookingsForSlotOnDate: repository null se trata como 0")
    void countBookings_repoReturnsNull_returnsZero() {
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_24_JUL))
                .thenReturn(null);

        int result = service.countBookingsForSlotOnDate(SLOT_ID, DATE_24_JUL);

        assertThat(result).isZero();
    }

    // ---------------- ensureSlotHasCapacity ----------------

    @Test
    @DisplayName("ensureSlotHasCapacity: tour unlimited pasa sin consultar reservas")
    void ensureCapacity_unlimited_shortCircuits() {
        Tour tour = new Tour();
        tour.setIsUnlimitedCapacity(true);
        TourScheduleConfigSlot slot = slot(SLOT_ID, 5);

        service.ensureSlotHasCapacity(tour, slot, DATE_24_JUL, 100);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    @DisplayName("ensureSlotHasCapacity: individual — pasa cuando hay capacidad en la fecha")
    void ensureCapacity_individual_passesWhenAvailable() {
        Tour tour = individualTour();
        TourScheduleConfigSlot slot = slot(SLOT_ID, 5);
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_24_JUL))
                .thenReturn(2);

        // 5 capacity - 2 booked en 24-jul = 3 available, pido 3
        service.ensureSlotHasCapacity(tour, slot, DATE_24_JUL, 3);
    }

    @Test
    @DisplayName("ensureSlotHasCapacity: individual — falla cuando no hay capacidad")
    void ensureCapacity_individual_failsWhenFull() {
        Tour tour = individualTour();
        TourScheduleConfigSlot slot = slot(SLOT_ID, 5);
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_24_JUL))
                .thenReturn(4);

        // 5 - 4 = 1 disponible, pido 2 → falla
        assertThatThrownBy(() -> service.ensureSlotHasCapacity(tour, slot, DATE_24_JUL, 2))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("Disponible: 1");
    }

    @Test
    @DisplayName("TC-004 REGRESIÓN: usa conteo por-fecha, NO el campo slot.bookings global")
    void ensureCapacity_ignoresGlobalSlotBookings() {
        Tour tour = individualTour();
        // slot.bookings=99 (contador global desactualizado del bug histórico) pero solo 1 reserva real hoy
        TourScheduleConfigSlot slot = slot(SLOT_ID, 5);
        slot.setBookings(99);
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_25_JUL))
                .thenReturn(1);

        // Si leyera slot.bookings=99 con capacity=5, siempre fallaría. Con conteo por-fecha=1, pasa.
        service.ensureSlotHasCapacity(tour, slot, DATE_25_JUL, 4);
    }

    @Test
    @DisplayName("TC-004 REGRESIÓN: fechas distintas del mismo slot no comparten bookings")
    void ensureCapacity_differentDates_independentCounts() {
        Tour tour = individualTour();
        TourScheduleConfigSlot slot = slot(SLOT_ID, 5);
        // 24-jul lleno (5), 25-jul vacío (0)
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_24_JUL))
                .thenReturn(5);
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_25_JUL))
                .thenReturn(0);

        // 24-jul rechaza
        assertThatThrownBy(() -> service.ensureSlotHasCapacity(tour, slot, DATE_24_JUL, 1))
                .isInstanceOf(OperationNotPermittedException.class);

        // 25-jul acepta el mismo pedido
        service.ensureSlotHasCapacity(tour, slot, DATE_25_JUL, 5);
    }

    @Test
    @DisplayName("ensureSlotHasCapacity: grupo — usa 1 unidad (no pax) al medir capacidad")
    void ensureCapacity_grupo_countsAsOneUnit() {
        Tour tour = grupoTour(10);
        TourScheduleConfigSlot slot = slot(SLOT_ID, 3);
        when(reservationRepository.countActiveBookingUnitsForSlotOnDate(SLOT_ID, DATE_24_JUL))
                .thenReturn(2);

        // capacity=3 grupos, booked=2 → 1 grupo disponible. Pido 8 pax (1 grupo, dentro del maxPeople=10)
        service.ensureSlotHasCapacity(tour, slot, DATE_24_JUL, 8);
    }

    @Test
    @DisplayName("ensureSlotHasCapacity: grupo — falla si pax excede maxPeople del tour")
    void ensureCapacity_grupo_failsWhenExceedsMaxPeople() {
        Tour tour = grupoTour(4);
        TourScheduleConfigSlot slot = slot(SLOT_ID, 10);

        assertThatThrownBy(() -> service.ensureSlotHasCapacity(tour, slot, DATE_24_JUL, 5))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("máximo de personas");
    }

    @Test
    @DisplayName("ensureSlotHasCapacity: falla si capacity del slot es null")
    void ensureCapacity_nullCapacity_fails() {
        Tour tour = individualTour();
        TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
        slot.setId(SLOT_ID);
        slot.setCapacity(null);

        assertThatThrownBy(() -> service.ensureSlotHasCapacity(tour, slot, DATE_24_JUL, 1))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("no tiene capacity configurada");
    }

    // ---------------- helpers ----------------

    private Tour individualTour() {
        Tour tour = new Tour();
        tour.setIsUnlimitedCapacity(false);
        tour.setPriceType(PriceTypeEnum.INDIVIDUAL);
        return tour;
    }

    private Tour grupoTour(int maxPeople) {
        Tour tour = new Tour();
        tour.setIsUnlimitedCapacity(false);
        tour.setPriceType(PriceTypeEnum.GRUPO);
        tour.setMaxPeople(maxPeople);
        return tour;
    }

    private TourScheduleConfigSlot slot(Integer id, int capacity) {
        TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
        slot.setId(id);
        slot.setCapacity(capacity);
        return slot;
    }
}

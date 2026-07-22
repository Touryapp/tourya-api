package com.tourya.api.jobs;

import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.models.Credit;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.repository.CreditRepository;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.services.TourScheduleSlotAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Hotfix #179b: garantiza que el job maneja reservas con {@code item_id NULL}
 * (huerfanas por FK ON DELETE SET NULL) y que un error en una reserva no
 * arrastra al resto (cada iteracion en su propia TX REQUIRES_NEW).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TemporalReservationExpiryJob")
class TemporalReservationExpiryJobTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ShoppingCartItemRepository shoppingCartItemRepository;
    @Mock private CreditRepository creditRepository;
    @Mock private TourScheduleSlotAvailabilityService tourScheduleSlotAvailabilityService;
    @Mock private PlatformTransactionManager transactionManager;

    private TemporalReservationExpiryJob job;

    @BeforeEach
    void setUp() {
        // TransactionManager mock devuelve un status funcional para que TransactionTemplate
        // ejecute el callback en cada iteracion.
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        job = new TemporalReservationExpiryJob(
                reservationRepository, shoppingCartItemRepository, creditRepository,
                tourScheduleSlotAvailabilityService, transactionManager);
    }

    @Test
    @DisplayName("no hace nada si no hay reservas expiradas")
    void noExpired_skipsBatch() {
        when(reservationRepository.findExpiredTemporalReservations(any())).thenReturn(List.of());

        job.expireTemporalReservations();

        verify(reservationRepository, never()).save(any());
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    @DisplayName("reserva con itemId=NULL: solo marca CANCELED, no toca item ni creditos")
    void expiredWithNullItemId_marksCanceledOnly() {
        Reservation r = temporalReservation(305L, null);
        when(reservationRepository.findExpiredTemporalReservations(any())).thenReturn(List.of(r));

        job.expireTemporalReservations();

        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(saved.capture());
        assertThat(saved.getValue().getDeliveryStatus()).isEqualTo(DeliveryStatusEnum.CANCELED);
        assertThat(saved.getValue().getExpiresAt()).isNull();
        assertThat(saved.getValue().getCancellationDate()).isNotNull();

        // No debe consultar item ni creditos si itemId es NULL
        verify(shoppingCartItemRepository, never()).findById(any());
        verify(creditRepository, never()).findByShoppingCartItemIdInAndStatusReserved(anySet());
    }

    @Test
    @DisplayName("reserva con itemId valido: cancela + libera item y creditos RESERVED")
    void expiredWithItemId_releasesItemAndCredits() {
        Reservation r = temporalReservation(400L, 487L);
        ShoppingCartItem item = new ShoppingCartItem();
        item.setId(487L);
        item.setReservationId(400L);
        Credit credit = new Credit();
        credit.setId(10L);
        credit.setStatus(CreditStatusEnum.RESERVED);
        credit.setReservedAmount(new BigDecimal("50.00"));
        credit.setShoppingCartItemId(487L);

        when(reservationRepository.findExpiredTemporalReservations(any())).thenReturn(List.of(r));
        when(shoppingCartItemRepository.findById(487L)).thenReturn(Optional.of(item));
        when(creditRepository.findByShoppingCartItemIdInAndStatusReserved(eq(Set.of(487L))))
                .thenReturn(new ArrayList<>(List.of(credit)));

        job.expireTemporalReservations();

        verify(reservationRepository).save(r);
        verify(shoppingCartItemRepository).save(item);
        assertThat(item.getReservationId()).isNull();

        ArgumentCaptor<List<Credit>> creditsCap = ArgumentCaptor.forClass(List.class);
        verify(creditRepository).saveAll(creditsCap.capture());
        Credit updated = creditsCap.getValue().get(0);
        assertThat(updated.getStatus()).isEqualTo(CreditStatusEnum.CREATED);
        assertThat(updated.getReservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updated.getShoppingCartItemId()).isNull();
    }

    @Test
    @DisplayName("hotfix: error en una reserva NO impide procesar las demas")
    void errorInOneReservation_doesNotBlockOthers() {
        Reservation ok1 = temporalReservation(500L, null);
        Reservation boom = temporalReservation(501L, 999L);
        Reservation ok2 = temporalReservation(502L, null);

        when(reservationRepository.findExpiredTemporalReservations(any())).thenReturn(List.of(ok1, boom, ok2));
        // El save de la reserva "boom" lanza — su tx REQUIRES_NEW se aisla,
        // el resto sigue vivo.
        when(reservationRepository.save(boom)).thenThrow(new RuntimeException("simulated DB error"));

        job.expireTemporalReservations();

        verify(reservationRepository).save(ok1);
        verify(reservationRepository).save(boom);
        verify(reservationRepository).save(ok2);
    }

    @Test
    @DisplayName("BE-27: reserva con item.slot → llama recalculate(slotId) tras cancelar")
    void expiredWithItemAndSlot_callsRecalculate() {
        Reservation r = temporalReservation(600L, 700L);
        ShoppingCartItem item = new ShoppingCartItem();
        item.setId(700L);
        TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
        slot.setId(1030);
        item.setSlot(slot);

        when(reservationRepository.findExpiredTemporalReservations(any())).thenReturn(List.of(r));
        when(shoppingCartItemRepository.findById(700L)).thenReturn(Optional.of(item));

        job.expireTemporalReservations();

        verify(tourScheduleSlotAvailabilityService).recalculate(1030);
    }

    @Test
    @DisplayName("BE-27: reserva con itemId=NULL NO llama recalculate (no hay slot que ajustar)")
    void expiredNullItemId_skipsRecalculate() {
        Reservation r = temporalReservation(601L, null);
        when(reservationRepository.findExpiredTemporalReservations(any())).thenReturn(List.of(r));

        job.expireTemporalReservations();

        verify(tourScheduleSlotAvailabilityService, never()).recalculate(any());
    }

    @Test
    @DisplayName("BE-27: si recalculate falla NO propaga — la reserva ya quedo cancelada")
    void expiredRecalculateFails_stillCancelsReservation() {
        Reservation r = temporalReservation(602L, 701L);
        ShoppingCartItem item = new ShoppingCartItem();
        item.setId(701L);
        TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
        slot.setId(1031);
        item.setSlot(slot);

        when(reservationRepository.findExpiredTemporalReservations(any())).thenReturn(List.of(r));
        when(shoppingCartItemRepository.findById(701L)).thenReturn(Optional.of(item));
        org.mockito.Mockito.doThrow(new RuntimeException("recalculate boom"))
                .when(tourScheduleSlotAvailabilityService).recalculate(1031);

        // No debe lanzar — la cancelacion es exitosa aunque el recalculate falle.
        job.expireTemporalReservations();

        verify(reservationRepository).save(r);
        assertThat(r.getDeliveryStatus()).isEqualTo(DeliveryStatusEnum.CANCELED);
    }

    private Reservation temporalReservation(Long id, Long itemId) {
        Reservation r = new Reservation();
        r.setReservationId(id);
        r.setItemId(itemId);
        r.setDeliveryStatus(DeliveryStatusEnum.TEMPORAL);
        r.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        return r;
    }
}

package com.tourya.api.services;

import com.tourya.api.constans.enums.CancellationPolicyTypeEnum;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.ShoppingCart;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCancellationPolicy;
import com.tourya.api.models.TourSchedule;
import com.tourya.api.models.User;
import com.tourya.api.models.request.ConfigQuantityRequest;
import com.tourya.api.models.request.RescheduleReservationRequest;
import com.tourya.api.models.responses.RescheduleValidationResponse;
import com.tourya.api.repository.ReservationRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.repository.TourCancellationPolicyRepository;
import com.tourya.api.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * BE-20 — Reschedule flow. Cubre guards de validateRescheduleReservation y las
 * primeras comprobaciones de rescheduleReservation (incluyendo el nuevo guard
 * de newDate en el pasado, cierre de RN-033).
 *
 * El flujo completo LOWER/EQUAL/HIGHER (createCredit, availability recalc,
 * addItemToCart, etc.) requiere un test integrado con @SpringBootTest +
 * Testcontainers y queda registrado como deuda futura (ver doc 00, BE-20b).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BE-20 ReservationService reschedule guards")
class ReservationServiceRescheduleTest {

    private static final Integer USER_ID = 42;
    private static final Long RESERVATION_ID = 100L;

    @Mock private ReservationRepository reservationRepository;
    @Mock private ShoppingCartItemRepository shoppingCartItemRepository;
    @Mock private TourCancellationPolicyRepository tourCancellationPolicyRepository;
    @Mock private TourRepository tourRepository;
    @Mock private com.tourya.api.repository.PaymentRepository paymentRepository;
    @Mock private com.tourya.api.models.mapper.ReservationMapper reservationMapper;
    @Mock private com.tourya.api.config.security.JwtService jwtService;
    @Mock private com.tourya.api.repository.TourScheduleRepository tourScheduleRepository;
    @Mock private com.tourya.api.services.ProviderService providerService;
    @Mock private com.tourya.api.repository.AccountPayableRepository accountPayableRepository;
    @Mock private com.tourya.api.repository.TourMainAttractionRepository tourMainAttractionRepository;
    @Mock private com.tourya.api.repository.TourIncludesExcludesRepository tourIncludesExcludesRepository;
    @Mock private com.tourya.api.repository.TourAddressRepository tourAddressRepository;
    @Mock private com.tourya.api.repository.TourGalleryRepository tourGalleryRepository;
    @Mock private com.tourya.api.repository.ReservationNativeRepository reservationNativeRepository;
    @Mock private com.tourya.api.repository.CreditRepository creditRepository;
    @Mock private com.tourya.api.repository.MaritimActivityReportRepository maritimActivityReportRepository;
    @Mock private com.tourya.api.repository.TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;
    @Mock private com.tourya.api.repository.TourScheduleConfigRepository tourScheduleConfigRepository;
    @Mock private com.tourya.api.services.AppConfigService appConfigService;
    @Mock private com.tourya.api.services.AgeRangeConfigService ageRangeConfigService;
    @Mock private com.tourya.api.services.ShoppingCartService shoppingCartService;
    @Mock private com.tourya.api.repository.ShoppingCartRepository shoppingCartRepository;
    @Mock private com.tourya.api.services.TourScheduleSlotAvailabilityService tourScheduleSlotAvailabilityService;
    @Mock private com.tourya.api.repository.ReviewRepository reviewRepository;
    @Mock private com.tourya.api.models.mapper.ReservationPriceBreakdownMapper reservationPriceBreakdownMapper;
    @Mock private com.tourya.api.repository.TouristProfileRepository touristProfileRepository;
    @Mock private com.tourya.api.services.TourPrincipalOperatorService tourPrincipalOperatorService;
    @Mock private com.tourya.api.models.mapper.TourAddressMapper tourAddressMapper;
    @Mock private com.tourya.api.models.mapper.TourIncludesExcludesMapper tourIncludesExcludesMapper;

    @Mock private Authentication authentication;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Reservation reservation;
    private ShoppingCartItem item;
    private Tour tour;
    private TourSchedule schedule;
    private TourCancellationPolicy policy;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail("test@tourya.co");

        tour = new Tour();
        tour.setId(1);

        schedule = new TourSchedule();
        schedule.setId(10);
        schedule.setTourId(tour.getId());

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);

        item = new ShoppingCartItem();
        item.setShoppingCart(cart);
        item.setTourSchedule(schedule);

        reservation = new Reservation();
        reservation.setReservationId(RESERVATION_ID);
        reservation.setItemId(500L);
        reservation.setDeliveryStatus(DeliveryStatusEnum.RESERVED);
        reservation.setCanReschedule(true);
        reservation.setMaxReschedulingDate(LocalDate.now().plusDays(30));
        reservation.setReservationDate(LocalDateTime.now().plusDays(10));

        policy = new TourCancellationPolicy();
        policy.setAllowsRescheduling(true);
        policy.setCancellationPolicyType(CancellationPolicyTypeEnum.FLEXIBLE);
    }

    // ==================== validateRescheduleReservation ====================

    @Test
    @DisplayName("validate: happy path — canReschedule=true when all conditions met")
    void validate_returnsCanReschedule_whenAllConditionsMet() {
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourCancellationPolicyRepository.findByTourId(tour.getId())).thenReturn(List.of(policy));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isTrue();
        assertThat(res.getReason()).isNull();
    }

    @Test
    @DisplayName("validate: canReschedule=false when reservation is CANCELED")
    void validate_returnsFalse_whenReservationCanceled() {
        reservation.setDeliveryStatus(DeliveryStatusEnum.CANCELED);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("ALREADY_CANCELED");
    }

    @Test
    @DisplayName("validate: canReschedule=false when reservation is DELIVERED")
    void validate_returnsFalse_whenReservationDelivered() {
        reservation.setDeliveryStatus(DeliveryStatusEnum.DELIVERED);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("ALREADY_DELIVERED");
    }

    @Test
    @DisplayName("validate: canReschedule=false when reservation was already RESCHEDULED (one-time only)")
    void validate_returnsFalse_whenAlreadyRescheduled() {
        reservation.setDeliveryStatus(DeliveryStatusEnum.RESCHEDULED);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("ALREADY_RESCHEDULED");
    }

    @Test
    @DisplayName("validate: canReschedule=false when canReschedule flag is false")
    void validate_returnsFalse_whenCanRescheduleFlagFalse() {
        reservation.setCanReschedule(false);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("RESCHEDULE_NOT_ALLOWED");
    }

    @Test
    @DisplayName("validate: canReschedule=false when reservation belongs to another user")
    void validate_returnsFalse_whenItemBelongsToDifferentUser() {
        User otherUser = new User();
        otherUser.setId(999);
        item.getShoppingCart().setUser(otherUser);

        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("PERMISSION_DENIED");
    }

    @Test
    @DisplayName("validate: canReschedule=false when policy does not allow rescheduling")
    void validate_returnsFalse_whenPolicyDoesNotAllowRescheduling() {
        policy.setAllowsRescheduling(false);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourCancellationPolicyRepository.findByTourId(tour.getId())).thenReturn(List.of(policy));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("RESCHEDULING_NOT_ALLOWED");
    }

    @Test
    @DisplayName("validate: canReschedule=false when policy list is empty")
    void validate_returnsFalse_whenPolicyMissing() {
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourCancellationPolicyRepository.findByTourId(tour.getId())).thenReturn(Collections.emptyList());

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("POLICY_NOT_FOUND");
    }

    @Test
    @DisplayName("validate: canReschedule=false when maxReschedulingDate has passed")
    void validate_returnsFalse_whenMaxReschedulingDatePassed() {
        reservation.setMaxReschedulingDate(LocalDate.now().minusDays(1));
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourCancellationPolicyRepository.findByTourId(tour.getId())).thenReturn(List.of(policy));

        RescheduleValidationResponse res = reservationService.validateRescheduleReservation(RESERVATION_ID, authentication);

        assertThat(res.getCanReschedule()).isFalse();
        assertThat(res.getReason()).isEqualTo("MAX_DATE_PASSED");
    }

    // ==================== rescheduleReservation guards ====================

    @Test
    @DisplayName("reschedule: throws when reservation was already RESCHEDULED (single reschedule rule)")
    void reschedule_throwsWhenAlreadyRescheduled() {
        reservation.setDeliveryStatus(DeliveryStatusEnum.RESCHEDULED);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() ->
                reservationService.rescheduleReservation(RESERVATION_ID, buildValidRequest(), authentication))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("ya fue reagendada");
    }

    @Test
    @DisplayName("reschedule: throws when reservation is already CANCELED")
    void reschedule_throwsWhenAlreadyCanceled() {
        reservation.setDeliveryStatus(DeliveryStatusEnum.CANCELED);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() ->
                reservationService.rescheduleReservation(RESERVATION_ID, buildValidRequest(), authentication))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    @DisplayName("reschedule: throws when reservation is already DELIVERED")
    void reschedule_throwsWhenAlreadyDelivered() {
        reservation.setDeliveryStatus(DeliveryStatusEnum.DELIVERED);
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() ->
                reservationService.rescheduleReservation(RESERVATION_ID, buildValidRequest(), authentication))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("completada");
    }

    @Test
    @DisplayName("reschedule: throws when newDate is in the past (BE-20 fix — RN-033)")
    void reschedule_throwsWhenNewDateIsInPast() {
        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourCancellationPolicyRepository.findByTourId(tour.getId())).thenReturn(List.of(policy));

        RescheduleReservationRequest req = buildValidRequest();
        req.setNewDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() ->
                reservationService.rescheduleReservation(RESERVATION_ID, req, authentication))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("past date");
    }

    @Test
    @DisplayName("reschedule: throws when belongs to a different user")
    void reschedule_throwsWhenBelongsToOtherUser() {
        User otherUser = new User();
        otherUser.setId(999);
        item.getShoppingCart().setUser(otherUser);

        when(authentication.getPrincipal()).thenReturn(user);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(shoppingCartItemRepository.findById(reservation.getItemId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() ->
                reservationService.rescheduleReservation(RESERVATION_ID, buildValidRequest(), authentication))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("permission");
    }

    private RescheduleReservationRequest buildValidRequest() {
        return RescheduleReservationRequest.builder()
                .newDate(LocalDate.now().plusDays(5))
                .configQuantity(List.of(
                        ConfigQuantityRequest.builder().ageType("ADULT").quantity(2).build()))
                .build();
    }
}

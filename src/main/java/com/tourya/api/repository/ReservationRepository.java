package com.tourya.api.repository;

import com.tourya.api.models.responses.ReservationDetailsResponse;
import com.tourya.api.models.Reservation;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import com.tourya.api.constans.enums.TourSubCategoryEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Reservation.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Busca reservas por URL del QR
     */
    Optional<Reservation> findByQrUrl(String qrUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.reservationId = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    /**
     * Busca reservas por ID del pago
     */
    List<Reservation> findByPaymentId(Long paymentId);

    /**
     * Busca reservas por estado de entrega
     */
    List<Reservation> findByDeliveryStatus(DeliveryStatusEnum deliveryStatus);

    /**
     * Busca reservas por fecha de reserva
     */
    List<Reservation> findByReservationDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca reservas por fecha de reserva específica
     */
    List<Reservation> findByReservationDate(LocalDateTime reservationDate);

    /**
     * MO-40 Fase D: reservas pagadas y pendientes de entrega cuya fecha de tour
     * cae en la fecha indicada. Se usa en el job de recordatorio 24h antes.
     */
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.deliveryStatus = com.tourya.api.constans.enums.DeliveryStatusEnum.PENDING " +
           "AND r.shoppingCartItem.tourSchedule.scheduleDate = :date")
    List<Reservation> findPendingForTourDate(@Param("date") LocalDate date);

    /**
     * Verifica si existe una reserva para un pago específico
     */
    boolean existsByPaymentId(Long paymentId);

    /**
     * Cuenta el número de reservas por estado de entrega
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.deliveryStatus = :deliveryStatus")
    long countByDeliveryStatus(@Param("deliveryStatus") DeliveryStatusEnum deliveryStatus);

    /**
     * Busca reservas creadas en un rango de fechas
     */
    @Query("SELECT r FROM Reservation r WHERE r.createdDate BETWEEN :startDate AND :endDate")
    List<Reservation> findByCreatedDateBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Busca reservas entregadas (DELIVERED) que no tienen una review asociada
     */
    @Query("""
        SELECT r FROM Reservation r 
        WHERE r.deliveryStatus = :deliveryStatus 
        AND NOT EXISTS (
            SELECT 1 FROM Review rev WHERE rev.reservationId = r.reservationId
        )
        ORDER BY r.reservationDate DESC
        """)
    Page<Reservation> findDeliveredWithoutReview(
            @Param("deliveryStatus") DeliveryStatusEnum deliveryStatus,
            Pageable pageable
    );

    /**
     * Busca reservas entregadas (DELIVERED) del usuario que no tienen una review asociada
     * Filtra por el userId del ShoppingCart asociado al ShoppingCartItem (dueño de la reserva)
     */
    @Query("""
        SELECT r FROM Reservation r 
        JOIN r.shoppingCartItem item
        JOIN item.shoppingCart cart
        WHERE r.deliveryStatus = :deliveryStatus 
        AND cart.user.id = :userId
        AND NOT EXISTS (
            SELECT 1 FROM Review rev WHERE rev.reservationId = r.reservationId
        )
        ORDER BY r.reservationDate DESC
        """)
    Page<Reservation> findDeliveredWithoutReviewByUserId(
            @Param("deliveryStatus") DeliveryStatusEnum deliveryStatus,
            @Param("userId") Integer userId,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.deliveryStatus = 'TEMPORAL'
          AND r.expiresAt IS NOT NULL
          AND r.expiresAt <= :now
        """)
    List<Reservation> findExpiredTemporalReservations(@Param("now") LocalDateTime now);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.reservationId IN :ids
        """)
    List<Reservation> findAllByReservationIdIn(@Param("ids") List<Long> ids);

    /**
     * BE-23: reservas afectadas por un reporte DIMAR bandera roja.
     * Une con shopping_cart_item → tour_schedule → tour, filtra por subcategoria,
     * rango de fechas del reporte y ubicacion via tour_address.
     * Solo trae reservas en estado abierto (excluye CANCELED, DELIVERED, etc.).
     */
    @Query("""
        SELECT r FROM Reservation r,
                    com.tourya.api.models.ShoppingCartItem sci,
                    com.tourya.api.models.TourSchedule ts,
                    com.tourya.api.models.Tour t
        WHERE r.itemId = sci.id
          AND sci.tourSchedule.id = ts.id
          AND ts.tour.id = t.id
          AND r.deliveryStatus IN :openStatuses
          AND t.subCategory = :subCategory
          AND ts.scheduleDate BETWEEN :startDate AND :endDate
          AND EXISTS (
              SELECT 1 FROM com.tourya.api.models.TourAddress ta
              WHERE ta.tour.id = t.id
                AND ta.country.id = :countryId
                AND ta.state.id = :stateId
                AND ta.city.id = :cityId
          )
        """)
    List<Reservation> findAffectedByRedAlert(
            @Param("subCategory") TourSubCategoryEnum subCategory,
            @Param("countryId") Integer countryId,
            @Param("stateId") Integer stateId,
            @Param("cityId") Integer cityId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("openStatuses") List<DeliveryStatusEnum> openStatuses);

    @Query("""
        SELECT r
        FROM Reservation r
        JOIN r.shoppingCartItem item
        WHERE r.deliveryStatus = :status
          AND item.scheduleDate IS NOT NULL
          AND item.scheduleDate < :today
        """)
    List<Reservation> findPendingWithScheduleDateBefore(
            @Param("status") DeliveryStatusEnum status,
            @Param("today") LocalDate today
    );

    @Query("""
        SELECT r FROM Reservation r
        WHERE (
            (r.canCancel = true AND r.maxCancellationDate IS NOT NULL AND r.maxCancellationDate < :today)
            OR (r.canReschedule = true AND r.maxReschedulingDate IS NOT NULL AND r.maxReschedulingDate < :today)
        )
        """)
    List<Reservation> findWithExpiredCancellationOrRescheduleFlags(@Param("today") LocalDate today);

    /**
     * Busca reservas general
     */
    @Query(
            value = """
        SELECT r.*
        FROM (
            SELECT *
            FROM sp_get_provider_reservations(:providerId, :reservationId, :deliveryStatus)
        ) AS r
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM (
            SELECT *
            FROM sp_get_provider_reservations(:providerId, :reservationId, :deliveryStatus)
        ) AS r
        """,
            nativeQuery = true
    )
    Page<ReservationDetailsResponse> getProviderReservations(
            @Param("providerId") Integer providerId,
            @Param("reservationId") Long reservationId,
            @Param("deliveryStatus") String deliveryStatus,
            Pageable pageable
    );



}
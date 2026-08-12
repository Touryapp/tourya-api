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
    /**
     * BE-23: reservas afectadas por alerta DIMAR bandera roja (native query).
     *
     * <p>Fix issue #193 (2026-07-23): antes era JPQL con {@code AND t.subCategory = :subCategory},
     * que fallaba con {@code operator does not exist: tour_subcategory_enum = character varying}
     * porque {@code tour.sub_category} es enum nativo Postgres y JPQL no aplica el
     * {@link org.hibernate.annotations.ColumnTransformer} de {@link com.tourya.api.models.Tour#subCategory}
     * al WHERE. Native query con CAST explícito lo soluciona.</p>
     *
     * <p>El bug era latente porque este código sólo se dispara al crear un reporte
     * {@code MaritimActivityReport} con {@code flag=RED} — evento que no había ocurrido en dev.</p>
     */
    @Query(value = """
        SELECT r.* FROM reservation r
            JOIN shopping_cart_item sci ON r.item_id = sci.id
            JOIN tour_schedule ts ON sci.tour_schedule_id = ts.id
            JOIN tour t ON ts.tour_id = t.id
        WHERE r.delivery_status = ANY(CAST(:openStatuses AS text[]))
          AND t.sub_category = CAST(:subCategory AS tour_subcategory_enum)
          AND ts.schedule_date BETWEEN :startDate AND :endDate
          AND EXISTS (
              SELECT 1 FROM tour_address ta
              WHERE ta.tour_id = t.id
                AND ta.country_id = :countryId
                AND ta.state_id = :stateId
                AND ta.city_id = :cityId
          )
        """, nativeQuery = true)
    List<Reservation> findAffectedByRedAlertNative(
            @Param("subCategory") String subCategory,
            @Param("countryId") Integer countryId,
            @Param("stateId") Integer stateId,
            @Param("cityId") Integer cityId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("openStatuses") String[] openStatuses);

    /**
     * BE-23: wrapper que convierte los enums al formato esperado por el native query.
     * Mantiene la firma tipada para no romper los callers.
     */
    default List<Reservation> findAffectedByRedAlert(
            TourSubCategoryEnum subCategory,
            Integer countryId,
            Integer stateId,
            Integer cityId,
            LocalDate startDate,
            LocalDate endDate,
            List<DeliveryStatusEnum> openStatuses) {
        String subCategoryValue = subCategory != null ? subCategory.getValue() : null;
        String[] statusNames = openStatuses == null ? new String[0]
                : openStatuses.stream().map(Enum::name).toArray(String[]::new);
        return findAffectedByRedAlertNative(subCategoryValue, countryId, stateId, cityId,
                startDate, endDate, statusNames);
    }

    // TC-011 (#206 reabierto Luis 2026-08-03): aceptar lista de estados para que
    // el job NO_SHOW procese tanto PENDING como RESCHEDULED (reservas reagendadas
    // cuya nueva fecha ya paso). Antes era `= :status` unico.
    @Query("""
        SELECT r
        FROM Reservation r
        JOIN r.shoppingCartItem item
        WHERE r.deliveryStatus IN :statuses
          AND item.scheduleDate IS NOT NULL
          AND item.scheduleDate < :today
        """)
    List<Reservation> findPendingWithScheduleDateBefore(
            @Param("statuses") List<DeliveryStatusEnum> statuses,
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

    /**
     * TC-004 + TC-019 (#231) bug 3: cuenta unidades reservadas para un slot en una fecha
     * específica.
     *
     * Suma unidades activas (TEMPORAL/PENDING/DELIVERED/RESCHEDULED) respetando priceType:
     *  - grupo: 1 unidad por reserva
     *  - individual: suma de pax (shopping_cart_item_detail.quantity)
     *
     * Se cuenta por (shopping_cart_item.slot_id, tour_schedule.schedule_date) para
     * evitar el bug del contador global slot.bookings (ver TC-004).
     *
     * RESCHEDULED cuenta como activa porque tras reagendar el item apunta al nuevo
     * (slot, schedule) y el cliente asistira ese día — omitirla habilita oversell en
     * la fecha destino (TC-019 #231 bug 3, Luis 2026-08-12). Debe mantenerse en sincronía
     * con el helper SQL {@code fn_slot_booked_units_on_schedule} (migración 089).
     */
    @Query(value = """
        SELECT COALESCE(SUM(
          CASE
            WHEN t.price_type = 'grupo' THEN 1
            ELSE COALESCE((
              SELECT SUM(d.quantity)
              FROM shopping_cart_item_detail d
              WHERE d.shopping_cart_item_id = i.id
            ), 0)
          END
        ), 0)
        FROM reservation r
        JOIN shopping_cart_item i ON i.id = r.item_id
        JOIN tour_schedule ts ON ts.id = i.tour_schedule_id
        JOIN tour t ON t.id = ts.tour_id
        WHERE i.slot_id = :slotId
          AND ts.schedule_date = :scheduleDate
          AND r.delivery_status IN ('TEMPORAL', 'PENDING', 'DELIVERED', 'RESCHEDULED')
        """,
        nativeQuery = true)
    Integer countActiveBookingUnitsForSlotOnDate(
            @Param("slotId") Integer slotId,
            @Param("scheduleDate") LocalDate scheduleDate);
}
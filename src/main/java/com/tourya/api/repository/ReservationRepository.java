package com.tourya.api.repository;

import com.tourya.api.models.responses.ReservationDetailsResponse;
import com.tourya.api.models.Reservation;
import com.tourya.api.constans.enums.DeliveryStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
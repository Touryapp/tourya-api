package com.tourya.api.repository;

import com.tourya.api.models.Payment;
import com.tourya.api.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Busca reservas por token QR
     */
    Optional<Reservation> findByQrToken(String qrToken);

    /**
     * Busca reservas por pago
     */
    List<Reservation> findByPayment(Payment payment);

    /**
     * Busca reservas por ID del pago
     */
    @Query("SELECT r FROM Reservation r WHERE r.payment.id = :paymentId")
    List<Reservation> findByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * Busca reservas por estado de entrega
     */
    List<Reservation> findByDeliveryStatus(String deliveryStatus);

    /**
     * Busca reservas por responsable del servicio
     */
    @Query("SELECT r FROM Reservation r WHERE r.serviceResponsibleId = :serviceResponsibleId")
    List<Reservation> findByServiceResponsibleId(@Param("serviceResponsibleId") Integer serviceResponsibleId);

    /**
     * Busca reservas por pagador
     */
    @Query("SELECT r FROM Reservation r WHERE r.payerId = :payerId")
    List<Reservation> findByPayerId(@Param("payerId") Integer payerId);

    /**
     * Busca reservas por email del responsable del servicio
     */
    List<Reservation> findByServiceResponsibleEmail(String serviceResponsibleEmail);

    /**
     * Busca reservas por email del pagador
     */
    List<Reservation> findByPayerEmail(String payerEmail);

    /**
     * Verifica si existe una reserva para un pago específico
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.payment.id = :paymentId")
    boolean existsByPaymentId(@Param("paymentId") Long paymentId);
}

package com.tourya.api.repository;

import com.tourya.api.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Payment.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Busca pagos por ID de transacción
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Busca pagos por ID de la reserva
     */
    List<Payment> findByReservationId(Long reservationId);

    /**
     * Busca pagos por ID del pagador
     */
    List<Payment> findByPayerId(Integer payerId);

    /**
     * Busca pagos por email del pagador
     */
    List<Payment> findByPayerEmail(String payerEmail);

    /**
     * Busca pagos por nombre del pagador
     */
    @Query("SELECT p FROM Payment p WHERE p.payerName LIKE CONCAT('%', :payerName, '%')")
    List<Payment> findByPayerNameContaining(@Param("payerName") String payerName);

    /**
     * Busca pagos por tipo de documento del pagador
     */
    List<Payment> findByPayerDocumentType(String documentType);

    /**
     * Busca pagos por número de documento del pagador
     */
    List<Payment> findByPayerDocumentNumber(String documentNumber);

    /**
     * Verifica si existe un pago para una reserva específica
     */
    boolean existsByReservationId(Long reservationId);

    /**
     * Cuenta el número de pagos por pagador
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payerId = :payerId")
    long countByPayerId(@Param("payerId") Integer payerId);
}
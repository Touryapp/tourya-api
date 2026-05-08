package com.tourya.api.repository;

import com.tourya.api.models.AccountPayable;
import com.tourya.api.constans.enums.AccountPayableStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad AccountPayable.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface AccountPayableRepository extends JpaRepository<AccountPayable, Long> {

    /**
     * Busca cuentas por pagar por ID de reserva
     */
    List<AccountPayable> findByReservationId(Long reservationId);

    /**
     * Busca cuentas por pagar por ID de proveedor
     */
    List<AccountPayable> findByProviderId(Integer providerId);

    /**
     * Busca cuentas por pagar por estado
     */
    List<AccountPayable> findByDeliveryStatus(AccountPayableStatusEnum deliveryStatus);

    /**
     * Busca cuentas por pagar por rango de fechas
     */
    @Query("SELECT ap FROM AccountPayable ap WHERE ap.transactionDate BETWEEN :startDate AND :endDate")
    List<AccountPayable> findByTransactionDateBetween(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Verifica si existe una cuenta por pagar para una reserva específica
     */
    boolean existsByReservationId(Long reservationId);

    /**
     * Busca cuenta por pagar por reserva y proveedor
     */
    Optional<AccountPayable> findByReservationIdAndProviderId(Long reservationId, Integer providerId);

    @Query("""
            SELECT ap
            FROM AccountPayable ap
            JOIN ap.reservation r
            JOIN r.shoppingCartItem sci
            WHERE ap.deliveryStatus = :status
              AND r.deliveryStatus = com.tourya.api.constans.enums.DeliveryStatusEnum.DELIVERED
              AND r.payoutAvailableDate IS NOT NULL
              AND r.payoutAvailableDate <= :payDate
              AND sci.scheduleDate BETWEEN :scheduleStart AND :scheduleEnd
            """)
    List<AccountPayable> findEligibleForPayoutOrder(
            @Param("status") AccountPayableStatusEnum status,
            @Param("payDate") LocalDate payDate,
            @Param("scheduleStart") LocalDate scheduleStart,
            @Param("scheduleEnd") LocalDate scheduleEnd
    );
}


package com.tourya.api.repository;

import com.tourya.api.models.Credit;
import com.tourya.api.constans.enums.CreditStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la entidad Credit.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {

    /**
     * Busca créditos por ID de reserva
     */
    List<Credit> findByReservationId(Long reservationId);

    /**
     * Busca créditos por estado
     */
    List<Credit> findByStatus(CreditStatusEnum status);

    /**
     * Busca créditos por estado y ordenados por fecha de creación
     */
    List<Credit> findByStatusOrderByCreationDateDesc(CreditStatusEnum status);

    /**
     * Busca créditos activos (creados) por ID de reserva
     */
    @Query("SELECT c FROM Credit c WHERE c.reservationId = :reservationId AND c.status = 'CREATED'")
    List<Credit> findActiveCreditsByReservationId(@Param("reservationId") Long reservationId);

    /**
     * Busca créditos que no han vencido
     */
    @Query("SELECT c FROM Credit c WHERE c.status = 'CREATED' AND c.expirationDate >= :date")
    List<Credit> findActiveCreditsNotExpired(@Param("date") LocalDate date);
}


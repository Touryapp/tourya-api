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

    /**
     * Busca créditos de un usuario específico.
     * Usa directamente el campo userId del crédito.
     * Opcionalmente puede filtrar por status del crédito.
     * 
     * @param userId ID del usuario
     * @param status Estado del crédito (opcional, si es null retorna todos)
     * @return Lista de créditos del usuario
     */
    @Query("""
        SELECT c FROM Credit c
        WHERE c.userId = :userId
        AND (:status IS NULL OR c.status = :status)
        """)
    List<Credit> findByUserId(@Param("userId") Integer userId, @Param("status") CreditStatusEnum status);
}


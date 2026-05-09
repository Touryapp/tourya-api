package com.tourya.api.repository;

import com.tourya.api.models.TouryaService;
import com.tourya.api.models.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Service.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ServiceRepository extends JpaRepository<TouryaService, Integer> {

    /**
     * Busca servicios por tipo de servicio.
     * 
     * @param serviceType tipo de servicio
     * @return lista de servicios
     */
    List<TouryaService> findByServiceType(ServiceType serviceType);

    /**
     * Busca servicios por ID del tipo de servicio.
     * 
     * @param serviceTypeId ID del tipo de servicio
     * @return lista de servicios
     */
    List<TouryaService> findByServiceTypeId(Integer serviceTypeId);

    /**
     * Busca servicios por estado.
     * 
     * @param status estado del servicio
     * @return lista de servicios
     */
    List<TouryaService> findByStatus(String status);

    /**
     * Busca servicios activos.
     * 
     * @return lista de servicios activos
     */
    @Query("SELECT s FROM TouryaService s WHERE s.status = 'ACTIVE'")
    List<TouryaService> findActiveServices();

    /**
     * Busca servicios activos por tipo de servicio.
     * 
     * @param serviceTypeId ID del tipo de servicio
     * @return lista de servicios activos
     */
    @Query("SELECT s FROM TouryaService s WHERE s.serviceType.id = :serviceTypeId AND s.status = 'ACTIVE'")
    List<TouryaService> findActiveServicesByType(@Param("serviceTypeId") Integer serviceTypeId);
}
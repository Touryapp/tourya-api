package com.tourya.api.repository;

import com.tourya.api.models.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad ServiceType.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, Integer> {

    /**
     * Busca un tipo de servicio por nombre.
     * 
     * @param name nombre del tipo de servicio
     * @return Optional con el tipo de servicio encontrado
     */
    Optional<ServiceType> findByName(String name);

    /**
     * Busca tipos de servicio por estado.
     * 
     * @param status estado del tipo de servicio
     * @return lista de tipos de servicio
     */
    List<ServiceType> findByStatus(String status);

    /**
     * Busca tipos de servicio activos.
     * 
     * @return lista de tipos de servicio activos
     */
    @Query("SELECT st FROM ServiceType st WHERE st.status = 'ACTIVE'")
    List<ServiceType> findActiveServiceTypes();
}



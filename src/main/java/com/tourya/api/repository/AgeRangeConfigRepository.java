package com.tourya.api.repository;

import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.models.AgeRangeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad AgeRangeConfig.
 * Proporciona métodos para acceder a la configuración de rangos de edad.
 */
@Repository
public interface AgeRangeConfigRepository extends JpaRepository<AgeRangeConfig, Integer> {

    /**
     * Busca una configuración de rango de edad por tipo de edad.
     *
     * @param ageType el tipo de edad (ADULT, CHILD, INFANT)
     * @return Optional con la configuración si existe
     */
    Optional<AgeRangeConfig> findByAgeType(AgePriceType ageType);

    /**
     * Obtiene todas las configuraciones de rangos de edad activas.
     *
     * @return lista de configuraciones activas
     */
    List<AgeRangeConfig> findByIsActiveTrue();

    /**
     * Verifica si existe una configuración para un tipo de edad específico.
     *
     * @param ageType el tipo de edad
     * @return true si existe, false en caso contrario
     */
    boolean existsByAgeType(AgePriceType ageType);
}

package com.tourya.api.repository;

import com.tourya.api.models.TourScheduleConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TourScheduleConfigRepository extends JpaRepository<TourScheduleConfig, Integer> {
    // Para asegurar que los slots se carguen con la configuración Y sus precios
    @Query("SELECT tsc FROM TourScheduleConfig tsc " +
            "LEFT JOIN FETCH tsc.slots s LEFT JOIN FETCH s.prices " +
            "WHERE tsc.id = :id")
    Optional<TourScheduleConfig> findByIdWithSlots(@Param("id") Integer id);

    Page<TourScheduleConfig> findByTourId(@Param("tourId") Integer tourId, Pageable pageable);

    // Consulta JPA para templates
    List<TourScheduleConfig> findByProviderIdAndIsTemplateTrue(Integer providerId);

    // Consulta por procedimiento almacenado en Postgres
    @Query(value = "SELECT * FROM get_templates_by_provider(:providerId)", nativeQuery = true)
    List<Object[]> findTemplatesByProviderSP(@Param("providerId") Integer providerId);
}


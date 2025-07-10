package com.tourya.api.repository;

import com.tourya.api.models.TourSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TourScheduleRepository extends JpaRepository<TourSchedule, Integer>, JpaSpecificationExecutor<TourSchedule> {
    List<TourSchedule> findByConfigId(Integer configId);

    Optional<TourSchedule> findByConfigIdAndScheduleDateAndStartTimeAndEndTime(
            Long configId, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime);

    void deleteByConfigId(Integer configId);

    // Consulta simplificada para evitar el error de validación.
    // Las relaciones se cargarán perezosamente o se gestionarán en el servicio.
    @Query("SELECT ts FROM TourSchedule ts WHERE ts.id IN :ids")
    List<TourSchedule> findAllByIdsWithDetails(@Param("ids") List<Integer> ids);
}

package com.tourya.api.repository;

import com.tourya.api.models.TourScheduleConfigSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TourScheduleConfigSlotRepository extends JpaRepository<TourScheduleConfigSlot, Integer> {
    // Para asegurar que los precios se carguen con el slot
    @Query("SELECT tscs FROM TourScheduleConfigSlot tscs LEFT JOIN FETCH tscs.prices WHERE tscs.id = :id")
    Optional<TourScheduleConfigSlot> findByIdWithPrices(@Param("id") Integer id);
}

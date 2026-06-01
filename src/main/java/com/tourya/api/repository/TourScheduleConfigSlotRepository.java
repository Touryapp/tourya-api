package com.tourya.api.repository;

import com.tourya.api.models.TourScheduleConfigSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TourScheduleConfigSlotRepository extends JpaRepository<TourScheduleConfigSlot, Integer> {
    @Query("SELECT tscs FROM TourScheduleConfigSlot tscs LEFT JOIN FETCH tscs.prices WHERE tscs.id = :id")
    Optional<TourScheduleConfigSlot> findByIdWithPrices(@Param("id") Integer id);

    @Query("""
            SELECT DISTINCT sl FROM TourScheduleConfigSlot sl
            JOIN FETCH sl.prices
            JOIN sl.config c
            WHERE c.tourId = :tourId
              AND EXISTS (
                SELECT 1 FROM TourSchedule ts
                WHERE ts.config.id = c.id
                  AND ts.scheduleDate >= :startDate
                  AND ts.scheduleDate <= :endDate
              )
            """)
    List<TourScheduleConfigSlot> findSlotsWithPricesByTourIdAndScheduleDateBetween(
            @Param("tourId") Integer tourId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT sl FROM TourScheduleConfigSlot sl
            JOIN FETCH sl.prices
            JOIN sl.config c
            WHERE sl.id = :slotId AND c.tourId = :tourId
            """)
    Optional<TourScheduleConfigSlot> findByIdAndTourIdWithPrices(
            @Param("slotId") Integer slotId,
            @Param("tourId") Integer tourId);
}

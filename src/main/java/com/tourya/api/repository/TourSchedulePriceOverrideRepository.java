package com.tourya.api.repository;

import com.tourya.api.models.TourSchedulePriceOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TourSchedulePriceOverrideRepository extends JpaRepository<TourSchedulePriceOverride, Integer> {

    Optional<TourSchedulePriceOverride> findByScheduleIdAndPriceId(Integer scheduleId, Integer priceId);

    List<TourSchedulePriceOverride> findByScheduleIdIn(Collection<Integer> scheduleIds);

    List<TourSchedulePriceOverride> findByScheduleId(Integer scheduleId);
}

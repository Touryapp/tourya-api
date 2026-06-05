package com.tourya.api.repository;

import com.tourya.api.models.TourScheduleSlotOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TourScheduleSlotOverrideRepository extends JpaRepository<TourScheduleSlotOverride, Integer> {

    Optional<TourScheduleSlotOverride> findByScheduleIdAndSlotId(Integer scheduleId, Integer slotId);

    List<TourScheduleSlotOverride> findByScheduleIdIn(Collection<Integer> scheduleIds);

    List<TourScheduleSlotOverride> findByScheduleId(Integer scheduleId);
}

package com.tourya.api.repository;

import com.tourya.api.models.TourSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TourScheduleRepository extends JpaRepository<TourSchedule, Integer> {
    List<TourSchedule> findByConfigId(Integer configId);

    Optional<TourSchedule> findByConfigIdAndScheduleDateAndStartTimeAndEndTime(
            Long configId, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime);

    void deleteByConfigId(Integer configId);
}

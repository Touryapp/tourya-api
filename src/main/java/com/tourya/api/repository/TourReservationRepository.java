package com.tourya.api.repository;

import com.tourya.api.models.TourReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TourReservationRepository extends JpaRepository<TourReservation, Integer>, JpaSpecificationExecutor<TourReservation> {
    Page<TourReservation> findByUserId(Integer userId, Pageable pageable);
}

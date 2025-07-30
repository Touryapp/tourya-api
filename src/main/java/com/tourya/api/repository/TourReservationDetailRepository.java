package com.tourya.api.repository;

import com.tourya.api.models.TourReservationDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourReservationDetailRepository extends JpaRepository<TourReservationDetail, Integer> {
}

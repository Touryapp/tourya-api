package com.tourya.api.repository;

import com.tourya.api.models.TourReservationDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourReservationDetailRepository extends JpaRepository<TourReservationDetail, Integer> {

    List<TourReservationDetail> findByPrice_Slot_Id(Integer slotId);
}

package com.tourya.api.repository;

import com.tourya.api.models.TourAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourAddressRepository extends JpaRepository<TourAddress, Integer> {
    List<TourAddress> findByTourId(Integer tourId);
    List<TourAddress> findByTourIdIn(List<Integer> tourIds);
}

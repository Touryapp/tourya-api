package com.tourya.api.repository;


import com.tourya.api.models.TourItinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourItineraryRepository extends JpaRepository<TourItinerary, Integer> {
    List<TourItinerary> findByTourId(Integer tourId);
    void deleteByTourId(Integer tourId);
}
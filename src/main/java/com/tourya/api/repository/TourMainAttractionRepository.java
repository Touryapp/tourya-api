package com.tourya.api.repository;

import com.tourya.api.models.TourMainAttraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface TourMainAttractionRepository extends JpaRepository<TourMainAttraction, Integer> {
    List<TourMainAttraction> findByTourId(Integer tourId);
}
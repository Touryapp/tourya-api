package com.tourya.api.repository;

import com.tourya.api.models.TourIncludesExcludes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourIncludesExcludesRepository extends JpaRepository<TourIncludesExcludes, Integer> {
    List<TourIncludesExcludes> findByTourId(Integer tourId);
}
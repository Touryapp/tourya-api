package com.tourya.api.repository;

import com.tourya.api.models.TourGallery;
import com.tourya.api.models.TourItinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourGalleryRepository extends JpaRepository<TourGallery, Long> {
    List<TourGallery> findByTourIdOrderByOrderIndexAsc(Integer tourId);
    List<TourGallery> findByTourId(Integer tourId);
    List<TourGallery> findByTourIdAndOrderIndex(Integer tourId, Integer orderIndex);
    void deleteByTourId(Integer tourId);
}
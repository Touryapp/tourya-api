package com.tourya.api.repository;

import com.tourya.api.models.TourFaq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourFaqRepository extends JpaRepository<TourFaq, Integer> {
    List<TourFaq> findByTourId(Integer tourId);
    void deleteByTourId(Integer tourId);
}

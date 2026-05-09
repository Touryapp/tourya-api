package com.tourya.api.repository;

import com.tourya.api.models.TourCancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourCancellationPolicyRepository extends JpaRepository<TourCancellationPolicy, Integer> {

    List<TourCancellationPolicy> findByTourId(Integer tourId);
    void deleteByTourId(Integer tourId);
}

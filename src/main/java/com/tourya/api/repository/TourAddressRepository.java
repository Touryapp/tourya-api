package com.tourya.api.repository;

import com.tourya.api.models.TourAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourAddressRepository extends JpaRepository<TourAddress, Integer> {
}

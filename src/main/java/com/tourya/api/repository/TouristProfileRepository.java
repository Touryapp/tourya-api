package com.tourya.api.repository;

import com.tourya.api.models.TouristProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TouristProfileRepository extends JpaRepository<TouristProfile, Long> {
    Optional<TouristProfile> findByUserId(Integer userId);
    boolean existsByUserId(Integer userId);
}


package com.tourya.api.repository;

import com.tourya.api.models.ProviderUserTour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderUserTourRepository extends JpaRepository<ProviderUserTour, Integer> {

    List<ProviderUserTour> findByProviderUserId(Integer providerUserId);

    @Query("""
            SELECT put FROM ProviderUserTour put
            JOIN FETCH put.tour
            WHERE put.providerUser.id = :providerUserId
            """)
    List<ProviderUserTour> findByProviderUserIdWithTour(@Param("providerUserId") Integer providerUserId);

    Optional<ProviderUserTour> findByProviderUserIdAndTourId(Integer providerUserId, Integer tourId);

    @Modifying
    @Query("""
            UPDATE ProviderUserTour put
            SET put.isPrincipal = false
            WHERE put.tour.id = :tourId
            """)
    void clearPrincipalForTour(@Param("tourId") Integer tourId);

    @Modifying
    @Query("DELETE FROM ProviderUserTour put WHERE put.providerUser.id = :providerUserId")
    void deleteByProviderUserId(@Param("providerUserId") Integer providerUserId);
}

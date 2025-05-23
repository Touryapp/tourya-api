package com.tourya.api.repository;

import com.tourya.api.models.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StateRepository extends JpaRepository<State, Integer> {

    @Query("""
            SELECT state
            FROM State state
            Where state.country.id = :countryId
            """)
    List<State> getAllStateByCountryId(@Param("countryId") Integer countryId);
}

package com.tourya.api.repository;

import com.tourya.api.models.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Integer> {
    @Query("""
            SELECT city
            FROM City city
            Where city.state.id = :stateId
            """)
    List<City> getAllCityByStateIdList(@Param("stateId") Integer stateId);
}

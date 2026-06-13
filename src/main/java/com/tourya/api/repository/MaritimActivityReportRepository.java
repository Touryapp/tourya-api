package com.tourya.api.repository;

import com.tourya.api.constans.enums.MaritimeFlagEnum;
import com.tourya.api.models.MaritimActivityReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaritimActivityReportRepository extends JpaRepository<MaritimActivityReport, Long> {

    @EntityGraph(attributePaths = {"country", "state", "city"})
    Page<MaritimActivityReport> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"country", "state", "city"})
    Optional<MaritimActivityReport> findById(Long id);

    @EntityGraph(attributePaths = {"country", "state", "city"})
    @Query("""
            SELECT m FROM MaritimActivityReport m
            WHERE :reportDate BETWEEN m.reportStartDate AND m.reportEndDate
            ORDER BY m.reportStartDate DESC
            """)
    List<MaritimActivityReport> findActiveOnDate(@Param("reportDate") LocalDate reportDate);

    @EntityGraph(attributePaths = {"country", "state", "city"})
    @Query("""
            SELECT m FROM MaritimActivityReport m
            WHERE m.country.id = :countryId
              AND m.state.id = :stateId
              AND m.city.id = :cityId
            ORDER BY m.reportStartDate DESC
            """)
    List<MaritimActivityReport> findByLocationIds(
            @Param("countryId") Integer countryId,
            @Param("stateId") Integer stateId,
            @Param("cityId") Integer cityId);

    @Query("""
            SELECT m FROM MaritimActivityReport m
            WHERE m.flag = :flag
              AND m.subcategoryCode = :subcategoryCode
              AND :reportDate BETWEEN m.reportStartDate AND m.reportEndDate
              AND m.country.id = :countryId
              AND m.state.id = :stateId
              AND m.city.id = :cityId
            """)
    List<MaritimActivityReport> findActiveRedReportsForSubcategoryAndLocation(
            @Param("flag") MaritimeFlagEnum flag,
            @Param("subcategoryCode") String subcategoryCode,
            @Param("reportDate") LocalDate reportDate,
            @Param("countryId") Integer countryId,
            @Param("stateId") Integer stateId,
            @Param("cityId") Integer cityId);
}

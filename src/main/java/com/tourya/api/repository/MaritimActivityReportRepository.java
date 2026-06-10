package com.tourya.api.repository;

import com.tourya.api.constans.enums.MaritimeFlagEnum;
import com.tourya.api.models.MaritimActivityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaritimActivityReportRepository extends JpaRepository<MaritimActivityReport, Long> {

    @Query("""
            SELECT m FROM MaritimActivityReport m
            WHERE :reportDate BETWEEN m.reportStartDate AND m.reportEndDate
            ORDER BY m.reportStartDate DESC
            """)
    List<MaritimActivityReport> findActiveOnDate(@Param("reportDate") LocalDate reportDate);

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

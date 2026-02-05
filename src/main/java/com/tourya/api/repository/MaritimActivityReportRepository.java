package com.tourya.api.repository;

import com.tourya.api.models.MaritimActivityReport;
import com.tourya.api.constans.enums.MaritimeFlagEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad MaritimActivityReport.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface MaritimActivityReportRepository extends JpaRepository<MaritimActivityReport, Long> {

    /**
     * Busca reportes por fecha
     */
    List<MaritimActivityReport> findByReportDate(LocalDate reportDate);

    /**
     * Busca reportes por país y ciudad
     */
    List<MaritimActivityReport> findByCountryAndCity(String country, String city);

    /**
     * Busca reportes por país, ciudad y fecha
     */
    Optional<MaritimActivityReport> findByCountryAndCityAndReportDate(String country, String city, LocalDate reportDate);

    /**
     * Busca reportes por país, ciudad, actividad y fecha
     */
    @Query("SELECT m FROM MaritimActivityReport m WHERE m.country = :country AND m.city = :city AND m.activity = :activity AND m.reportDate = :reportDate")
    Optional<MaritimActivityReport> findByCountryAndCityAndActivityAndReportDate(
            @Param("country") String country,
            @Param("city") String city,
            @Param("activity") String activity,
            @Param("reportDate") LocalDate reportDate
    );

    /**
     * Busca reportes por bandera
     */
    List<MaritimActivityReport> findByFlag(MaritimeFlagEnum flag);
}


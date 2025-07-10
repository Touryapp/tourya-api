package com.tourya.api.models.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TourSearchRequestDto {
    private String keyword; // Busca en tour.name, tour.description
    private Long categoryId;
    private Integer minAge;
    private BigDecimal minRating;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate searchDate; // Fecha específica para buscar horarios
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateRange; // Rango de fechas de disponibilidad
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDateRange; // Rango de fechas de disponibilidad

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime; // Hora de inicio del slot
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime; // Hora de fin del slot

    private Integer minCapacityAvailable; // Capacidad disponible mínima (max_capacity - reserved_capacity)
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String ageType; // age_type del precio (ej. "ADULT", "CHILD")

    private Long countryId;
    private Long stateId;
    private Long cityId;
    private String addressKeyword; // Busca en tour_address.address o tour_address.location

    private int page = 0; // Número de página (0-indexed)
    private int size = 10; // Tamaño de la página
    private String sortBy = "scheduleDate"; // Campo para ordenar
    private String sortDir = "asc"; // Dirección de ordenación ("asc" o "desc")
}

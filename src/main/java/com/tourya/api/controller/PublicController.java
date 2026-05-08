package com.tourya.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.responses.*;
import com.tourya.api.services.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
@RestController
@RequestMapping("public")
@RequiredArgsConstructor
@Tag(name = "Public")
public class PublicController {
    private final CountryService countryService;
    private final StateService stateService;
    private final CityService cityService;
    private final SearchTourScheduleFullService searchTourScheduleFullService;
    private final SearchTourLocationService SearchTourLocationService;
    private final SearchTourCategoryService SearchTourCategoryService;
    private final TagCategoryService tagCategoryService;
    private final TourTagService tourTagService;
    private final TourService tourService;
    private final ReservationService reservationService;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @GetMapping("/country/getAllCountryList")
    @Operation(operationId = "publicGetAllCountries", summary = "Listar países (público)")
    public ResponseEntity<List<CountryResponse>> getAllCountryList() {
        return ResponseEntity.ok(countryService.getAllCountryList());
    }

    @GetMapping("/state/getAllStateByCountryIdList/{countryId}")
    @Operation(operationId = "publicGetStatesByCountry", summary = "Listar estados por país (público)")
    public ResponseEntity<List<StateResponse>> getAllStateByCountryIdList(
            @PathVariable Integer countryId) {
        return ResponseEntity.ok(stateService.getAllStateByCountryIdList(countryId));
    }

    @GetMapping("/city/getAllCityByStateIdList/{stateId}")
    @Operation(operationId = "publicGetCitiesByState", summary = "Listar ciudades por estado (público)")
    public ResponseEntity<List<CityResponse>> getAllCityByStateIdList(
            @PathVariable Integer stateId) {
        return ResponseEntity.ok(cityService.getAllCityByStateIdList(stateId));
    }

    @PostMapping("/tours/schedule/search")
    @Operation(
            operationId = "publicToursScheduleSearch",
            summary = "Búsqueda pública de tours/schedules",
            description = "Ejecuta búsqueda vía stored procedure `sp_get_tour_schedule_json`. Soporta filtros: categoryId, subCategory, durationEnum, timeOfDay, rango de precio, tags, rango de fechas y requestedUnits para validar cupos."
    )
    public ResponseEntity<Page<SearchTourScheduleFullResponse>> search(
            @RequestBody PublicTourScheduleSearchRequest filters,
            @ParameterObject
            @Parameter(description = "Paginación (page, size, sort) como query params")
            Pageable pageable) {
        return ResponseEntity.ok(searchTourScheduleFullService.searchTourSchedule(filters, pageable));
    }

    @GetMapping("/search/locations")
    @Operation(operationId = "publicSearchLocations", summary = "Listar ubicaciones disponibles para búsqueda (público)")
    public ResponseEntity<List<SearchTourLocationResponse>> getTourLocations() {
        return ResponseEntity.ok(SearchTourLocationService.getTourLocations());
    }

    @GetMapping("/search/categories")
    @Operation(operationId = "publicSearchCategories", summary = "Listar categorías disponibles para búsqueda (público)")
    public ResponseEntity<List<SearchTourCategoryResponse>> getTourCategories() {
        return ResponseEntity.ok(SearchTourCategoryService.getTourCategories());
    }

    @GetMapping("/search/subcategories")
    @Operation(operationId = "publicSearchSubcategories", summary = "Listar subcategorías disponibles para búsqueda (público)")
    public ResponseEntity<List<SearchTourSubCategoryResponse>> getTourSubCategories() {
        return ResponseEntity.ok(SearchTourCategoryService.getTourSubCategories());
    }

    @GetMapping("tag/categories")
    @Operation(operationId = "publicGetTagCategories", summary = "Listar categorías de tags (público)")
    public ResponseEntity<List<TagCategoryResponse>> getCategories() {
        return ResponseEntity.ok(tagCategoryService.getCategories());
    }

    @GetMapping("tags")
    @Operation(operationId = "publicGetAllTags", summary = "Listar tags (público)")
    public ResponseEntity<List<TourTagResponse>> getAllTags() {
        return ResponseEntity.ok(tourTagService.getAllTags());
    }

    @GetMapping("/age-price-types")
    @Operation(operationId = "publicGetAgePriceTypes", summary = "Listar tipos de edad para precios (público)")
    public AgePriceType[] getAgePriceTypes() {
        return AgePriceType.values();
    }

    @GetMapping("/consultDataTourById/{tourId}")
    @Operation(operationId = "publicConsultDataTourById", summary = "Consultar datos completos de un tour por ID (público/según auth)")
    public ResponseEntity<TourFullDataResponse> consultDataTourById(
            @PathVariable Integer tourId, Authentication connectedUser) {
        return ResponseEntity.ok(tourService.consultDataTourById(tourId, connectedUser));
    }

    // --- NEW UNIFIED ENDPOINT ---
    @GetMapping("/tour/details/{tourId}")
    @Operation(operationId = "publicGetTourDetails", summary = "Obtener detalles de un tour (público; auth opcional)")
    public ResponseEntity<TourFullDataResponse> getTourDetails(
            @PathVariable Integer tourId,
            @Nullable Authentication connectedUser) {
        return ResponseEntity.ok(tourService.getTourDetailsById(tourId, connectedUser));
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(operationId = "publicGetBookingById", summary = "Obtener booking por ID (público)")
    public ResponseEntity<com.tourya.api.models.responses.BookingDetailsResponse> getBookingById(
            @PathVariable Long bookingId) {
        // El bookingId es el reservationId según el mock
        return ResponseEntity.ok(reservationService.getBookingDetailsById(bookingId));
    }

}

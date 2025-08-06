package com.tourya.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.models.responses.*;
import com.tourya.api.services.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("public")
@RequiredArgsConstructor
@Tag(name = "Public")
public class    PublicController {
    private final CountryService countryService;
    private final StateService stateService;
    private final CityService cityService;
    private final SearchTourScheduleFullService searchTourScheduleFullService;
    private final SearchTourLocationService SearchTourLocationService;
    private final SearchTourCategoryService SearchTourCategoryService;
    private final TagCategoryService tagCategoryService;
    private final TourTagService tourTagService;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @GetMapping("/country/getAllCountryList")
    public ResponseEntity<List<CountryResponse>> getAllCountryList(){
        return ResponseEntity.ok(countryService.getAllCountryList());
    }

    @GetMapping("/state/getAllStateByCountryIdList/{countryId}")
    public ResponseEntity<List<StateResponse>> getAllStateByCountryIdList(
            @PathVariable Integer countryId
            ){
        return ResponseEntity.ok(stateService.getAllStateByCountryIdList(countryId));
    }

    @GetMapping("/city/getAllCityByStateIdList/{stateId}")
    public ResponseEntity<List<CityResponse>> getAllCityByStateIdList(
            @PathVariable Integer stateId
    ){
        return ResponseEntity.ok(cityService.getAllCityByStateIdList(stateId));
    }


    @PostMapping("/tours/schedule/search")
    public ResponseEntity<Page<SearchTourScheduleFullResponse>> search(
            @RequestBody Map<String, Object> filters,
            Pageable pageable) {
        return ResponseEntity.ok(searchTourScheduleFullService.searchTourSchedule(filters, pageable));
    }

    @GetMapping("/search/locations")
    public ResponseEntity<List<SearchTourLocationResponse>> getTourLocations() {
        return  ResponseEntity.ok(SearchTourLocationService.getTourLocations());
    }

    @GetMapping("/search/categories")
    public ResponseEntity<List<SearchTourCategoryResponse>>getTourCategories() {
        return ResponseEntity.ok(SearchTourCategoryService.getTourCategories());
    }

    @GetMapping("tag/categories")
    public ResponseEntity<List<TagCategoryResponse>> getCategories() {
        return ResponseEntity.ok(tagCategoryService.getCategories());
    }

    @GetMapping("tags")
    public ResponseEntity<List<TourTagResponse>> getAllTags() {
        return ResponseEntity.ok(tourTagService.getAllTags());
    }

    @GetMapping("/age-price-types")
    public AgePriceType[] getAgePriceTypes() {
        return AgePriceType.values();
    }


}

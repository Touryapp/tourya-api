package com.tourya.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.CityResponse;
import com.tourya.api.models.responses.CountryResponse;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.models.responses.StateResponse;
import com.tourya.api.services.CityService;
import com.tourya.api.services.CountryService;
import com.tourya.api.services.SearchTourScheduleFullService;
import com.tourya.api.services.StateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("public")
@RequiredArgsConstructor
@Tag(name = "Public")
public class PublicController {
    private final CountryService countryService;
    private final StateService stateService;
    private final CityService cityService;
    private final SearchTourScheduleFullService service;
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
    public List<SearchTourScheduleFullResponse> search(@RequestBody Map<String, Object> filters) {
        return service.searchTourSchedule(filters);
    }

}

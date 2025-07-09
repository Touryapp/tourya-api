package com.tourya.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.services.SearchTourScheduleFullService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tour/schedule")
@RequiredArgsConstructor
public class SearchTourScheduleFullController {

    private final SearchTourScheduleFullService service;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/search")
    public List<SearchTourScheduleFullResponse> search(@RequestBody Map<String, Object> filters) {
        return service.searchTourSchedule(filters);
    }
}
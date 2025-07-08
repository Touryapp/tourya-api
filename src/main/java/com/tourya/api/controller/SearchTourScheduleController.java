package com.tourya.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.request.SearchTourScheduleRequest;
import com.tourya.api.models.responses.SearchTourScheduleResponse;
import com.tourya.api.services.SearchTourScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tours/schedule")
@RequiredArgsConstructor
public class SearchTourScheduleController {

    private final SearchTourScheduleService service;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/search")
    public List<SearchTourScheduleResponse> search(@RequestBody Map<String, Object> body) {
        SearchTourScheduleRequest request = objectMapper.convertValue(body, SearchTourScheduleRequest.class);
        return service.searchTourSchedule(request);
    }
}
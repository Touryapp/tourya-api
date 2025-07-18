package com.tourya.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.services.SearchTourScheduleFullService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tour/schedule")
@RequiredArgsConstructor
public class SearchTourScheduleFullController {

    private final SearchTourScheduleFullService searchTourScheduleFullService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/tours/schedule/search")
    public ResponseEntity<Page<SearchTourScheduleFullResponse>> search(
            @RequestBody Map<String, Object> filters,
            Pageable pageable) {
        return ResponseEntity.ok(searchTourScheduleFullService.searchTourSchedule(filters, pageable));
    }
}
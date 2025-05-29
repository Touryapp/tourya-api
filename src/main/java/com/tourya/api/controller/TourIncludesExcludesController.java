package com.tourya.api.controller;

import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.models.request.TourIncludesExcludesRequest;
import com.tourya.api.models.responses.TourIncludesExcludesResponse;
import com.tourya.api.services.TourIncludesExcludesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tours/{tourId}/includes-excludes")
@RequiredArgsConstructor
public class TourIncludesExcludesController {

    private final TourIncludesExcludesService tourIncludesExcludesService;

    // Reemplazo total
    @PostMapping("/replace")
    public ResponseEntity<List<TourIncludesExcludesResponse>> replaceAll(
            @PathVariable Integer tourId,
            @RequestBody List<TourIncludesExcludesRequest> requestList,
            @RequestParam(name = "type") IncludeExcludeTypeEnum type,
            Authentication authentication) {

        List<TourIncludesExcludesResponse> result = tourIncludesExcludesService.replaceAllForTour(requestList, tourId, type, authentication);
        return ResponseEntity.ok(result);
    }

    // Consulta todos los elementos por tour
    @GetMapping
    public ResponseEntity<List<TourIncludesExcludesResponse>> getAllByTour(
            @PathVariable Integer tourId,@RequestParam(name = "type", required = false) IncludeExcludeTypeEnum type) {

        List<TourIncludesExcludesResponse> result = tourIncludesExcludesService.getAllByTour(tourId, type);
        return ResponseEntity.ok(result);
    }
}

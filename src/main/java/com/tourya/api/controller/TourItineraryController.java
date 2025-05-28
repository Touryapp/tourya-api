package com.tourya.api.controller;


import com.tourya.api.models.responses.TourItineraryResponse;
import com.tourya.api.models.responses.TourMainAttractionResponse;
import com.tourya.api.models.resquest.TourItineraryRequest;
import com.tourya.api.models.resquest.TourMainAttractionRequest;
import com.tourya.api.services.TourItineraryService;
import com.tourya.api.services.TourMainAttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tours/{tourId}/itinerary")
@RequiredArgsConstructor
public class TourItineraryController {

    private final TourItineraryService tourItineraryService;

    // Reemplazo total
    @PostMapping("/replace")
    public ResponseEntity<List<TourItineraryResponse>> replaceAll(
            @PathVariable Integer tourId,
            @RequestBody List<TourItineraryRequest> requestList,
            Authentication authentication) {

        List<TourItineraryResponse> result = tourItineraryService.replaceAllForTour(requestList, tourId, authentication);
        return ResponseEntity.ok(result);
    }

    // Consulta todos los elementos por tour
    @GetMapping
    public ResponseEntity<List<TourItineraryResponse>> getAllByTour(
            @PathVariable Integer tourId) {

        List<TourItineraryResponse> result = tourItineraryService.getAllByTour(tourId);
        return ResponseEntity.ok(result);
    }
}
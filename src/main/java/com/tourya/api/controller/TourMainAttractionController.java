package com.tourya.api.controller;

import com.tourya.api.models.resquest.TourMainAttractionRequest;
import com.tourya.api.models.responses.TourMainAttractionResponse;
import com.tourya.api.services.TourMainAttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tours/{tourId}/main-attractions")
@RequiredArgsConstructor
public class TourMainAttractionController {

    private final TourMainAttractionService tourMainAttractionService;

    // Reemplazo total
    /*@PostMapping("/replace")
    public ResponseEntity<List<TourMainAttractionResponse>> replaceAll(
            @PathVariable Integer tourId,
            @RequestBody List<TourMainAttractionRequest> requestList,
            Authentication authentication) {

        List<TourMainAttractionResponse> result = tourMainAttractionService.replaceAllForTour(requestList, tourId, authentication);
        return ResponseEntity.ok(result);
    }*/

    // Consulta todos los elementos por tour
    @GetMapping
    public ResponseEntity<List<TourMainAttractionResponse>> getAllByTour(
            @PathVariable Integer tourId) {

        List<TourMainAttractionResponse> result = tourMainAttractionService.getAllByTour(tourId);
        return ResponseEntity.ok(result);
    }
}

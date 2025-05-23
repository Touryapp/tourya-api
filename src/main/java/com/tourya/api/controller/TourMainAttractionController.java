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
@RequestMapping("/api/tour-main-attractions")
@RequiredArgsConstructor
public class TourMainAttractionController {

    private final TourMainAttractionService tourMainAttractionService;

    @PostMapping("/{tourId}")
    public ResponseEntity<List<TourMainAttractionResponse>> create(
            @PathVariable Integer tourId,
            @RequestBody List<TourMainAttractionRequest> requestList,
            Authentication connectedUser) {
        return ResponseEntity.ok(tourMainAttractionService.create(requestList, tourId, connectedUser));
    }

    @GetMapping("/tour/{tourId}")
    public ResponseEntity<List<TourMainAttractionResponse>> getByTour(@PathVariable Integer tourId) {
        return ResponseEntity.ok(tourMainAttractionService.getAllByTour(tourId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        //TODO
        //tourMainAttractionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
package com.tourya.api.controller;

import com.tourya.api.models.resquest.TourIncludesExcludesRequest;
import com.tourya.api.models.responses.TourIncludesExcludesResponse;
import com.tourya.api.services.TourIncludesExcludesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tour-includes-excludes")
@RequiredArgsConstructor
public class TourIncludesExcludesController {

    private final TourIncludesExcludesService tourIncludesExcludesService;

    @PostMapping("/{tourId}")
    public ResponseEntity<List<TourIncludesExcludesResponse>> create(
            @PathVariable Integer tourId,
            @RequestBody List<TourIncludesExcludesRequest> requestList,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(tourIncludesExcludesService.create(requestList, tourId, connectedUser));
    }

    @GetMapping("/tour/{tourId}")
    public ResponseEntity<List<TourIncludesExcludesResponse>> getByTour(@PathVariable Integer tourId) {
        return ResponseEntity.ok(tourIncludesExcludesService.getAllByTour(tourId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        //tourIncludesExcludesService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
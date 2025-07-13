package com.tourya.api.controller;

import com.tourya.api.common.PageResponse;
import com.tourya.api.models.User;
import com.tourya.api.models.request.ReservationRequest;
import com.tourya.api.models.responses.ReservationResponse;
import com.tourya.api.services.TourReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations")
public class TourReservationController {

    private final TourReservationService tourReservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication
    ) {
        User connectedUser = (User) authentication.getPrincipal();
        ReservationResponse response = tourReservationService.createReservation(request, connectedUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable Integer reservationId,
            Authentication authentication
    ) {
        User connectedUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(tourReservationService.getReservationById(reservationId, connectedUser));
    }

    @GetMapping("/my-reservations")
    public ResponseEntity<PageResponse<ReservationResponse>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate,desc") String[] sort,
            Authentication authentication
    ) {
        User connectedUser = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sort[1]), sort[0]));
        return ResponseEntity.ok(tourReservationService.getAllReservationsForUser(connectedUser, pageable));
    }

    @PutMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Integer reservationId,
            Authentication authentication
    ) {
        User connectedUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(tourReservationService.cancelReservation(reservationId, connectedUser));
    }
}

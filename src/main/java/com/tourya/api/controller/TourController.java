package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.models.Tour;
import com.tourya.api.models.responses.TourDetailsResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.resquest.TourCreateRequest;
import com.tourya.api.models.resquest.TourRequest;
import com.tourya.api.services.TourService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("tour")
@RequiredArgsConstructor
@Tag(name = "Tour")
public class TourController {
    private final TourService tourService;

    @PostMapping("/user/save")
    public ResponseEntity<TourResponse> save(
            @Valid @RequestBody TourRequest tourRequest,
            Authentication connectedUser
            ){
        return ResponseEntity.ok(tourService.save(tourRequest, connectedUser));
    }

    @GetMapping("/user/findAllByUser")
    public ResponseEntity<PageResponse<TourResponse>> findAllByUser (
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser){
        return ResponseEntity.ok(tourService.findAllByUser(page, size, connectedUser));
    }
    @PostMapping("/user/saveCreate")
    public ResponseEntity<TourDetailsResponse> saveCreate(
            @Valid @RequestBody TourCreateRequest tourCreateRequest,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourService.saveCreate(tourCreateRequest, connectedUser));
    }
}

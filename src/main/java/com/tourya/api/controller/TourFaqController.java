package com.tourya.api.controller;

import com.tourya.api.models.responses.TourFaqResponse;
import com.tourya.api.models.request.TourFaqRequest;
import com.tourya.api.services.TourFaqService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("tourFaq")
@RequiredArgsConstructor
@Tag(name = "TourFaq")
public class TourFaqController {
    private final TourFaqService tourFaqService;

    @PostMapping("/user/save/{tourId}")
    public ResponseEntity<TourFaqResponse> save(
            @Valid @RequestBody TourFaqRequest tourFaqRequest,
            @PathVariable Integer tourId,
            Authentication connectedUser
            ){
        return ResponseEntity.ok(tourFaqService.saveTourFaqByTourId(tourFaqRequest, tourId, connectedUser));
    }

    @PostMapping("/user/saveList/{tourId}")
    public ResponseEntity<List<TourFaqResponse>> saveList(
            @Valid @RequestBody List<TourFaqRequest> tourFaqRequestList,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourFaqService.saveTourFaqListByTourId(tourFaqRequestList, tourId, connectedUser));
    }

    @PostMapping("/user/replaceAll/{tourId}")
    public ResponseEntity<List<TourFaqResponse>> replaceAll(
            @Valid @RequestBody List<TourFaqRequest> tourFaqRequestList,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourFaqService.replaceAll(tourFaqRequestList, tourId, connectedUser));
    }
}

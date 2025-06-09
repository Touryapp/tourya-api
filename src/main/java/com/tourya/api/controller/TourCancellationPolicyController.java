package com.tourya.api.controller;


import com.tourya.api.models.request.TourCancellationPolicyRequest;
import com.tourya.api.models.responses.TourCancellationPolicyResponse;
import com.tourya.api.services.TourCancellationPolicyService;
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
@RequestMapping("tourCancellationPolicy")
@RequiredArgsConstructor
@Tag(name = "TourCancellationPolicy")
public class TourCancellationPolicyController {
    private final TourCancellationPolicyService tourCancellationPolicyService;

    @PostMapping("/user/save/{tourId}")
    public ResponseEntity<TourCancellationPolicyResponse> save(
            @Valid @RequestBody TourCancellationPolicyRequest tourFaqRequest,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourCancellationPolicyService.saveCancellationPolicyByTourId(tourFaqRequest, tourId, connectedUser));
    }

    @PostMapping("/user/saveList/{tourId}")
    public ResponseEntity<List<TourCancellationPolicyResponse>> saveList(
            @Valid @RequestBody List<TourCancellationPolicyRequest> tourCancellationPolicyRequestList,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourCancellationPolicyService.saveTourCancellationPolicyListByTourId(tourCancellationPolicyRequestList, tourId, connectedUser));
    }

    @PostMapping("/user/replaceAll/{tourId}")
    public ResponseEntity<List<TourCancellationPolicyResponse>> replaceAll(
            @Valid @RequestBody List<TourCancellationPolicyRequest> tourCancellationPolicyRequestList,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourCancellationPolicyService.replaceAll(tourCancellationPolicyRequestList, tourId, connectedUser));
    }
}

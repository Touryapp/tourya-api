package com.tourya.api.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.models.responses.TourFullDataResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.request.TourFullDataRequest;
import com.tourya.api.models.request.TourRequest;
import com.tourya.api.services.TourService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("tour")
@RequiredArgsConstructor
@Tag(name = "Tour")
public class TourController {
    private final TourService tourService;
    private final ObjectMapper objectMapper;

    @PostMapping("/user/save")
    public ResponseEntity<TourResponse> save(
            @Valid @RequestBody TourRequest tourRequest,
            Authentication connectedUser
            ){
        return ResponseEntity.ok(tourService.save(tourRequest, connectedUser));
    }
    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<TourResponse>> findAll (
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "status", required = false) TourStatusEnum status,
            Authentication connectedUser){
        return ResponseEntity.ok(tourService.findAll(page, size, status, connectedUser));
    }
    @GetMapping("/admin/consultDataTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> consultDataTourByIdToAdmin (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.consultDataTourByIdToAdmin(tourId, connectedUser));
    }
    @PutMapping("/admin/acceptTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> acceptTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.acceptTourByIdToAdmin(tourId, connectedUser));
    }
    @PutMapping("/admin/cancelTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> cancelTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.cancelTourByIdToAdmin(tourId, connectedUser));
    }
    @GetMapping("/user/findAllByUser")
    public ResponseEntity<PageResponse<TourResponse>> findAllByUser (
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser){
        return ResponseEntity.ok(tourService.findAllByUser(page, size, connectedUser));
    }
    /*
    @PostMapping("/user/saveCreateBasicData")
    public ResponseEntity<TourDetailsResponse> saveCreateBasicData(
            @Valid @RequestBody TourCreateRequest tourCreateRequest,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourService.saveCreateBasicData(tourCreateRequest, connectedUser));
    }*/
 /*   @PostMapping("/user/saveAll")
    public ResponseEntity<TourFullDataResponse> saveCreateFullData(
            @Valid @RequestBody TourFullDataRequest tourFullDataRequest,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourService.saveCreateOrUpdateFullData(tourFullDataRequest, connectedUser));
    }*/
    @GetMapping("/user/consultDataTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> consultDataTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.consultDataTourById(tourId, connectedUser));
    }

    @PostMapping(value = "/user/saveAll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TourFullDataResponse> saveCreateFullData(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("metadata") String metadataJson,
            Authentication connectedUser
    ) throws IOException {
        TourFullDataRequest tourFullDataRequest = objectMapper.readValue(metadataJson, new TypeReference<>() {});
        return ResponseEntity.ok(tourService.saveCreateOrUpdateFullData(files,
                tourFullDataRequest, connectedUser));
    }
}

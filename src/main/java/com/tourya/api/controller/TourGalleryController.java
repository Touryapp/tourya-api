
package com.tourya.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.request.TourGalleryRequest;
import com.tourya.api.models.responses.TourGalleryResponse;
import com.tourya.api.services.TourGalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/tours/{tourId}/gallery")
@RequiredArgsConstructor
public class TourGalleryController {

    private final TourGalleryService tourGalleryService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TourGalleryResponse>> create(
            @PathVariable Integer tourId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("metadata") String metadataJson,
            Authentication connectedUser
    ) throws IOException {
        List<TourGalleryRequest> metadataList = objectMapper.readValue(metadataJson, new TypeReference<>() {});
        return ResponseEntity.ok(tourGalleryService.create(files, metadataList, tourId, connectedUser));
    }

    @GetMapping
    public ResponseEntity<List<TourGalleryResponse>> getGallery(
            @PathVariable Integer tourId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(tourGalleryService.getAllByTour(tourId, connectedUser));
    }

    @PostMapping(value = "/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TourGalleryResponse>> replaceGallery(
            @PathVariable Integer tourId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("metadata") String metadataJson,
            Authentication connectedUser
    ) throws IOException {
        List<TourGalleryRequest> metadataList = objectMapper.readValue(metadataJson, new TypeReference<>() {});
        return ResponseEntity.ok(tourGalleryService.replaceAllForTour(files, metadataList, tourId, connectedUser));
    }
}

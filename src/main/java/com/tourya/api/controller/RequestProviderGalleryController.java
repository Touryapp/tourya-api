package com.tourya.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.request.RequestProviderGalleryRequest;
import com.tourya.api.models.responses.RequestProviderGalleryResponse;
import com.tourya.api.services.RequestProviderGalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/requestProvider/{requestId}/gallery")
@RequiredArgsConstructor
public class RequestProviderGalleryController {

    private final RequestProviderGalleryService requestProviderGalleryService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<RequestProviderGalleryResponse>> create(
            @PathVariable Integer requestId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("metadata") String metadataJson,
            Authentication connectedUser
    ) throws IOException {
        List<RequestProviderGalleryRequest> metadataList = objectMapper.readValue(metadataJson, new TypeReference<>() {});
        return ResponseEntity.ok(requestProviderGalleryService.create(files, metadataList, requestId, connectedUser));
    }

    @GetMapping
    public ResponseEntity<List<RequestProviderGalleryResponse>> getGallery(
            @PathVariable Integer requestId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(requestProviderGalleryService.getAllByRequest(requestId, connectedUser));
    }

    @PostMapping(value = "/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<RequestProviderGalleryResponse>> replaceGallery(
            @PathVariable Integer requestId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("metadata") String metadataJson,
            Authentication connectedUser
    ) throws IOException {
        List<RequestProviderGalleryRequest> metadataList = objectMapper.readValue(metadataJson, new TypeReference<>() {});
        return ResponseEntity.ok(requestProviderGalleryService.replaceAll(files, metadataList, requestId, connectedUser));
    }
}

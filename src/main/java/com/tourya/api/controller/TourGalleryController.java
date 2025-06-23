
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

    @GetMapping
    public ResponseEntity<List<TourGalleryResponse>> getGallery(
            @PathVariable Integer tourId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(tourGalleryService.getAllByTour(tourId, connectedUser));
    }

    /**
     * Sincroniza la galería de un tour basada en el estado deseado.
     * El frontend debe enviar:
     * - 'newFiles': Las nuevas imágenes a subir.
     * - 'galleryData': Un JSON que contiene la metadata para TODAS las imágenes deseadas:
     * - Para imágenes existentes, incluir su 'id'.
     * - Para nuevas imágenes (cuyos archivos están en 'newFiles'), NO incluir 'id' (o null).
     * El orden de estas solicitudes sin 'id' debe coincidir con el orden de los 'newFiles'.
     */
    @PostMapping(value = "/sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TourGalleryResponse>> syncGallery(
            @PathVariable Integer tourId,
            @RequestPart(value = "newFiles", required = false) List<MultipartFile> newFiles,
            @RequestPart("galleryData") String galleryDataJson,
            Authentication connectedUser
    ) throws IOException {
        List<TourGalleryRequest> galleryRequests = objectMapper.readValue(galleryDataJson, new TypeReference<>() {});

        List<TourGalleryResponse> updatedGallery = tourGalleryService.syncTourGallery(
                tourId, newFiles, galleryRequests, connectedUser
        );
        return ResponseEntity.ok(updatedGallery);
    }

    @PostMapping(value = "/syncWithUpdate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TourGalleryResponse>> syncGalleryWithUpdate(
            @PathVariable Integer tourId,
            // 'files' es un Map donde la clave es el 'fileKey' y el valor es el MultipartFile.
            // Esto permite que el frontend asocie cualquier archivo (nuevo o de reemplazo) a su TourGalleryRequest
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart("galleryData") String galleryDataJson,
            Authentication connectedUser
    ) throws IOException {
        List<TourGalleryRequest> galleryRequests = objectMapper.readValue(galleryDataJson, new TypeReference<>() {});

        List<TourGalleryResponse> updatedGallery = tourGalleryService.syncTourGalleryWithUpdate(
                tourId, files, galleryRequests, connectedUser
        );
        return ResponseEntity.ok(updatedGallery);
    }
}

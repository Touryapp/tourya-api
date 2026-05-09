package com.tourya.api.controller;

import com.tourya.api.models.request.TouristProfileUpsertRequest;
import com.tourya.api.models.responses.TouristProfileAddressCompleteResponse;
import com.tourya.api.models.responses.TouristProfileResponse;
import com.tourya.api.services.TouristProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/tourist/profile")
@RequiredArgsConstructor
@Tag(name = "Tourist Profile")
public class TouristProfileController {

    private final TouristProfileService touristProfileService;

    @GetMapping
    @Operation(operationId = "touristGetMyProfile", summary = "Obtener mi perfil de turista")
    public ResponseEntity<TouristProfileResponse> getMyProfile(Authentication connectedUser) {
        return ResponseEntity.ok(touristProfileService.getMyProfile(connectedUser));
    }

    @PutMapping
    @Operation(operationId = "touristUpsertMyProfile", summary = "Crear/actualizar mi perfil de turista")
    public ResponseEntity<TouristProfileResponse> upsertMyProfile(
            @Valid @RequestBody TouristProfileUpsertRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(touristProfileService.upsertMyProfile(request, connectedUser));
    }

    @DeleteMapping
    @Operation(operationId = "touristDeleteMyProfile", summary = "Eliminar mi perfil de turista")
    public ResponseEntity<Void> deleteMyProfile(Authentication connectedUser) {
        touristProfileService.deleteMyProfile(connectedUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/address-complete")
    @Operation(operationId = "touristIsAddressComplete", summary = "Indica si mi dirección está completa (ciudad/estado/país)")
    public ResponseEntity<TouristProfileAddressCompleteResponse> isAddressComplete(Authentication connectedUser) {
        return ResponseEntity.ok(
                TouristProfileAddressCompleteResponse.builder()
                        .addressComplete(touristProfileService.isMyAddressComplete(connectedUser))
                        .build()
        );
    }

    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "touristUploadPhoto", summary = "Subir/actualizar mi foto (imagen <= 1MB)")
    public ResponseEntity<TouristProfileResponse> uploadMyPhoto(
            @RequestPart("file") MultipartFile file,
            Authentication connectedUser
    ) throws IOException {
        return ResponseEntity.ok(touristProfileService.uploadMyPhoto(file, connectedUser));
    }
}


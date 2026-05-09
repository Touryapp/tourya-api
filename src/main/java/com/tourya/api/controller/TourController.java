package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.models.responses.TourCompleteDataResponse;
import com.tourya.api.models.responses.TourFullDataResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.services.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.tourya.api.models.request.TourFullDataRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("tour")
@RequiredArgsConstructor
@Tag(name = "Tour")
public class TourController {
    private final TourService tourService;

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
    @PutMapping("/admin/returnedTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> returnedTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.returnedTourByIdToAdmin(tourId, connectedUser));
    }

    @PutMapping("/user/submitTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> submitTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.submitTourByIdToProvider(tourId, connectedUser));
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
    @PostMapping("/user/saveAll")
    @Operation(
            summary = "Crear/actualizar Tour (full data)",
            description = "Crea o actualiza un tour. Incluye nuevos campos: priceType, isUnlimitedCapacity, subCategory, durationEnum, timeOfDay. " +
                    "Nota UI: `maxPeople` aplica cuando `priceType = GROUP`."
    )
    public ResponseEntity<TourFullDataResponse> saveCreateFullData(
            @Valid @RequestBody TourFullDataRequest tourFullDataRequest,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourService.saveCreateOrUpdateFullData(tourFullDataRequest, connectedUser));
    }
    @GetMapping("/user/consultDataTourById/{tourId}")
    public ResponseEntity<TourFullDataResponse> consultDataTourById (
            @PathVariable Integer tourId, Authentication connectedUser){
        return ResponseEntity.ok(tourService.consultDataTourById(tourId, connectedUser));
    }

    // --- NEW UNIFIED ENDPOINT ---
    @GetMapping("/details/{tourId}")
    @Operation(summary = "Detalle unificado del Tour", description = "Retorna el detalle del tour (para público o autenticado).")
    public ResponseEntity<TourFullDataResponse> getTourDetails(
            @PathVariable Integer tourId,
            @Nullable Authentication connectedUser
    ) {
        return ResponseEntity.ok(tourService.getTourDetailsById(tourId, connectedUser));
    }
}

package com.tourya.api.controller;


import com.tourya.api.models.responses.TourAddressResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.resquest.TourAddressRequest;
import com.tourya.api.models.resquest.TourRequest;
import com.tourya.api.services.TourAddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("tourAddress")
@RequiredArgsConstructor
@Tag(name = "TourAddress")
public class TourAddressController {

    public final TourAddressService tourAddressService;

    @PostMapping("/user/save/{tourId}")
    public ResponseEntity<TourAddressResponse> save(
            @Valid @RequestBody TourAddressRequest tourAddressRequest,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourAddressService.saveTourAddressByTourId(tourAddressRequest, tourId, connectedUser));
    }

    @PostMapping("/user/saveList/{tourId}")
    public ResponseEntity<List<TourAddressResponse>> saveList(
            @Valid @RequestBody List<TourAddressRequest> tourAddressRequestList,
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourAddressService.saveTourAddressListByTourId(tourAddressRequestList, tourId, connectedUser));
    }

    @GetMapping("/user/consultDataTourAddressById/{tourAddressId}")
    public ResponseEntity<TourAddressResponse> consultDataTourAddressById (
            @PathVariable Integer tourAddressId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourAddressService.consultDataTourAddressById(tourAddressId, connectedUser));
    }
    @GetMapping("/user/consultDataTourAddressListByTourId/{tourId}")
    public ResponseEntity<List<TourAddressResponse>> consultDataTourAddressListByTourId (
            @PathVariable Integer tourId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(tourAddressService.consultDataTourAddressListByTourId(tourId, connectedUser));
    }

}

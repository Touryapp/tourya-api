package com.tourya.api.controller;



import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.SolicitudStatusEnum;
import com.tourya.api.models.responses.SolicitudResponse;
import com.tourya.api.models.resquest.SolicitudRequest;
import com.tourya.api.services.SolicitudService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("solicitud")
@RequiredArgsConstructor
@Tag(name = "Solicitud")
public class SolicitudController {
    private final SolicitudService solicitudService;

    @PostMapping("/user/save")
    public ResponseEntity<SolicitudResponse> saveSolicitud(
            @Valid @RequestBody SolicitudRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.save(request, connectedUser));
    }

    @GetMapping("/user/consultData")
    public ResponseEntity<SolicitudResponse> consultData(
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.consultData(connectedUser));
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<SolicitudResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "status", required = false) SolicitudStatusEnum status,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.findAll(page, size, status, connectedUser));
    }

    @GetMapping("/admin/consultDataById/{solicitudId}")
    public ResponseEntity<SolicitudResponse> consultDataById(
            @PathVariable Integer solicitudId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.consultDataById(solicitudId, connectedUser));
    }
    @PutMapping("/admin/approve/{solicitudId}")
    public ResponseEntity<SolicitudResponse> approveSolicitudById(
            @PathVariable Integer solicitudId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.approveSolicitudById(solicitudId, connectedUser));
    }
    @PutMapping("/admin/decline/{solicitudId}")
    public ResponseEntity<SolicitudResponse> declineSolicitudById(
            @PathVariable Integer solicitudId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.declineSolicitudById(solicitudId, connectedUser));
    }
}

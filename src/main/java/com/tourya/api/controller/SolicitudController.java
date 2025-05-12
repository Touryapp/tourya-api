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

    @PostMapping
    public ResponseEntity<SolicitudResponse> saveSolicitud(
            @Valid @RequestBody SolicitudRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.save(request, connectedUser));
    }

    @GetMapping
    public ResponseEntity<SolicitudResponse> getSolicitudByUser(
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.getSolicitudByUser(connectedUser));
    }

    @GetMapping("/allByStatus")
    public ResponseEntity<PageResponse<SolicitudResponse>> getSolicitudAllByStatus(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "status", required = false) SolicitudStatusEnum status,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.getSolicitudesAllByStatus(page, size, status, connectedUser));
    }

    @GetMapping("/{solicitudId}")
    public ResponseEntity<SolicitudResponse> getSolicitudById(
            @PathVariable Integer solicitudId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.getSolicitudById(solicitudId, connectedUser));
    }
    @PutMapping("/aprobar/{solicitudId}")
    public ResponseEntity<SolicitudResponse> aprobarSolicitudById(
            @PathVariable Integer solicitudId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.aprobarSolicitudById(solicitudId, connectedUser));
    }
    @PutMapping("/declinar/{solicitudId}")
    public ResponseEntity<SolicitudResponse> declinarSolicitudById(
            @PathVariable Integer solicitudId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(solicitudService.declinarSolicitudById(solicitudId, connectedUser));
    }
}

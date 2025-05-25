package com.tourya.api.controller;



import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.RequestProviderStatusEnum;
import com.tourya.api.models.responses.RequestProviderResponse;
import com.tourya.api.models.resquest.RequestProviderRequest;
import com.tourya.api.services.RequestProviderService;
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
@RequestMapping("requestProvider")
@RequiredArgsConstructor
@Tag(name = "RequestProvider")
public class RequestProviderController {
    private final RequestProviderService requestProviderService;

    @PostMapping("/user/save")
    public ResponseEntity<RequestProviderResponse> saveRequestProvider(
            @Valid @RequestBody RequestProviderRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(requestProviderService.save(request, connectedUser));
    }

    @GetMapping("/user/consultData")
    public ResponseEntity<RequestProviderResponse> consultData(
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(requestProviderService.consultData(connectedUser));
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<RequestProviderResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "status", required = false) RequestProviderStatusEnum status,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(requestProviderService.findAll(page, size, status, connectedUser));
    }

    @GetMapping("/admin/consultDataById/{requestProviderById}")
    public ResponseEntity<RequestProviderResponse> consultDataById(
            @PathVariable Integer requestProviderById,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(requestProviderService.consultDataById(requestProviderById, connectedUser));
    }
    @PutMapping("/admin/approve/{requestProviderById}")
    public ResponseEntity<RequestProviderResponse> approveRequestProviderById(
            @PathVariable Integer requestProviderById,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(requestProviderService.approveRequestProviderById(requestProviderById, connectedUser));
    }
    @PutMapping("/admin/decline/{requestProviderById}")
    public ResponseEntity<RequestProviderResponse> declineRequestProviderById(
            @PathVariable Integer requestProviderById,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(requestProviderService.declineRequestProviderById(requestProviderById, connectedUser));
    }
}

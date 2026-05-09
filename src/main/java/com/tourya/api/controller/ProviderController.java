package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.models.responses.ProviderResponse;
import com.tourya.api.models.request.ProviderRequest;
import com.tourya.api.services.ProviderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("provider")
@RequiredArgsConstructor
@Tag(name = "Provider")
public class ProviderController {

    private final ProviderService providerService;

    @PutMapping("/user/update")
    public ResponseEntity<ProviderResponse> updateProvider(@Valid @RequestBody ProviderRequest providerRequest,
                                                            Authentication connectedUser){
        return ResponseEntity.ok(providerService.update(providerRequest, connectedUser));
    }

    @GetMapping("/user/consultData")
    public ResponseEntity<ProviderResponse> consultDataProvider(Authentication connectedUser){
        return ResponseEntity.ok(providerService.consultDataProvider(connectedUser));
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<ProviderResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "status", required = false) ProviderStatusEnum status,
            Authentication connectedUser){
        return ResponseEntity.ok(providerService.findAll(page, size, status, connectedUser));
    }

    @GetMapping("/admin/consultDataById/{providerId}")
    public ResponseEntity<ProviderResponse> consultDataProviderById(@PathVariable Integer providerId, Authentication connectedUser){
        return ResponseEntity.ok(providerService.consultDataProviderById(providerId, connectedUser));
    }
    @DeleteMapping("/admin/deleteById/{providerId}")
    public void deleteProviderById(@PathVariable Integer providerId, Authentication connectedUser){
        providerService.deleteProviderById(providerId, connectedUser);
    }

    @PutMapping("/admin/activeOrInactiveById/{providerId}")
    public ResponseEntity<ProviderResponse> inactiveProviderById(@PathVariable Integer providerId,
                                                                  @RequestParam(name = "status") ProviderStatusEnum status,
                                                                  Authentication connectedUser){
        return ResponseEntity.ok(providerService.activeOrInactiveProviderById(providerId, status, connectedUser));
    }
}

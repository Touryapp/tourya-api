package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ProveedorStatusEnum;
import com.tourya.api.models.responses.ProveedorResponse;
import com.tourya.api.models.resquest.ProveedorRequest;
import com.tourya.api.services.ProveedorService;
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
@RequestMapping("proveedor")
@RequiredArgsConstructor
@Tag(name = "Proveedor")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @PutMapping("/user/update")
    public ResponseEntity<ProveedorResponse> updateProveedor(@Valid @RequestBody ProveedorRequest proveedorRequest,
                                                             Authentication connectedUser){
        return ResponseEntity.ok(proveedorService.update(proveedorRequest, connectedUser));
    }

    @GetMapping("/user/consultData")
    public ResponseEntity<ProveedorResponse> consultDataProveedor(Authentication connectedUser){
        return ResponseEntity.ok(proveedorService.consultDataProveedor(connectedUser));
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<ProveedorResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "status", required = false) ProveedorStatusEnum status,
            Authentication connectedUser){
        return ResponseEntity.ok(proveedorService.findAll(page, size, status, connectedUser));
    }

    @GetMapping("/admin/consultDataById/{proveedorId}")
    public ResponseEntity<ProveedorResponse> consultDataProveedorById(@PathVariable Integer proveedorId, Authentication connectedUser){
        return ResponseEntity.ok(proveedorService.consultDataProveedorById(proveedorId, connectedUser));
    }
    @DeleteMapping("/admin/deleteById/{proveedorId}")
    public void deleteProveedorById(@PathVariable Integer proveedorId, Authentication connectedUser){
        proveedorService.delecteProveedorById(proveedorId, connectedUser);
    }

    @PutMapping("/admin/activeOrInactiveById/{proveedorId}")
    public ResponseEntity<ProveedorResponse> inactiveProveedorById(@PathVariable Integer proveedorId,
                                                                   @RequestParam(name = "status") ProveedorStatusEnum status,
                                                                   Authentication connectedUser){
        return ResponseEntity.ok(proveedorService.activeOrInactiveProveedorById(proveedorId, status, connectedUser));
    }
}

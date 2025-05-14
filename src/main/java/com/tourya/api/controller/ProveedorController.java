package com.tourya.api.controller;


import com.tourya.api.models.responses.ProveedorResponse;
import com.tourya.api.models.resquest.ProveedorRequest;
import com.tourya.api.services.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("proveedor")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @PutMapping()
    public ProveedorResponse updateProveedor(@Valid @RequestBody ProveedorRequest proveedorRequest,
                                             Authentication connectedUser){

        return proveedorService.update(proveedorRequest, connectedUser);
    }

    @GetMapping()
    public ProveedorResponse consultarProveedor(Authentication connectedUser){
        return proveedorService.consultarProveedor(connectedUser);
    }
}

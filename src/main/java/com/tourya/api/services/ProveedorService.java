package com.tourya.api.services;


import com.tourya.api.models.Proveedor;
import com.tourya.api.models.User;
import com.tourya.api.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProveedorService {
    private final ProveedorRepository proveedorRepository;

    public Proveedor save(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public Proveedor findByUser(User user) {
        return proveedorRepository.findByUser(user);
    }
}

package com.tourya.api.repository;

import com.tourya.api.models.Proveedor;
import com.tourya.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
     Proveedor findByUser(User user);
}

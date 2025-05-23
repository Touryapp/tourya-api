package com.tourya.api.repository;

import com.tourya.api.constans.enums.ProveedorStatusEnum;
import com.tourya.api.constans.enums.SolicitudStatusEnum;
import com.tourya.api.models.Proveedor;
import com.tourya.api.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
     Proveedor findByUser(User user);

     @Query("""
            SELECT proveedor
            FROM Proveedor proveedor
            WHERE ( ((:status) IS NULL ) OR ( proveedor.status = :status ) )
            """)
     Page<Proveedor> findAllProveedor(@Param("status") ProveedorStatusEnum status, Pageable pageable);
}

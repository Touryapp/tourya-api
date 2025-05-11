package com.tourya.api.repository;


import com.tourya.api.models.Proveedor;
import com.tourya.api.models.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {
    Solicitud findByProveedor(Proveedor proveedor);

    @Query("""
            SELECT solicitud
            FROM Solicitud solicitud
            WHERE solicitud.status = 'solicitado'
            """)
    Page<Solicitud> findAllSolicitudesPendientes(Pageable pageable);
}

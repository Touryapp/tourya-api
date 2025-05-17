package com.tourya.api.repository;


import com.tourya.api.constans.enums.SolicitudStatusEnum;
import com.tourya.api.models.Proveedor;
import com.tourya.api.models.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {
    Solicitud findByProveedor(Proveedor proveedor);

    @Query("""
            SELECT solicitud
            FROM Solicitud solicitud
            WHERE ((:status IS NULL ) OR (solicitud.status = :status))
            """)
    Page<Solicitud> findAllSolicitudesPendientes(@Param("status") SolicitudStatusEnum status, Pageable pageable);
}

package com.tourya.api.repository;

import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourRepository extends JpaRepository<Tour, Integer> {

    @Query("""
            SELECT tour
            FROM Tour tour
            Where tour.proveedor.id = :id
            """)
    Page<Tour> findAllByProveedorId(@Param("id") Integer id,  Pageable pageable);

    @Query("""
            SELECT tour
            FROM Tour tour
            Where tour.id = :id And tour.proveedor.id = :proveedorId
            """)
    Tour findTourByIdAndProveedorId(@Param("id") Integer id, @Param("proveedorId") Integer proveedorId);

}

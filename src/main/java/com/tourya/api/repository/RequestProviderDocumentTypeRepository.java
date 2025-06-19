package com.tourya.api.repository;

import com.tourya.api.models.RequestProviderDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RequestProviderDocumentTypeRepository extends JpaRepository<RequestProviderDocumentType, Integer> {

    @Query("""
            SELECT requestProviderDocumentType
            FROM RequestProviderDocumentType requestProviderDocumentType
            """)
    List<RequestProviderDocumentType> getAllRequestProviderDocumentTypeList();
}

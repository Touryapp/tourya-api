package com.tourya.api.repository;

import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import com.tourya.api.models.ReviewReasonCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReasonCatalogRepository extends JpaRepository<ReviewReasonCatalog, Integer> {

    List<ReviewReasonCatalog> findByReasonTypeOrderByReasonIdAsc(ReviewReasonTypeEnum reasonType);
}

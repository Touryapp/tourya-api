package com.tourya.api.repository;

import com.tourya.api.models.RequestProviderGallery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestProviderGalleryRepository extends JpaRepository<RequestProviderGallery, Integer> {

    List<RequestProviderGallery> findByRequestProviderIdOrderByOrderIndexAsc(Integer requestProviderId);

    List<RequestProviderGallery> findByRequestProviderId(Integer requestProviderId);

    void deleteByRequestProviderId(Integer requestProviderId);
}

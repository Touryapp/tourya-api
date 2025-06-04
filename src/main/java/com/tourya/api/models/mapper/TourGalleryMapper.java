package com.tourya.api.models.mapper;

import com.tourya.api.models.Tour;
import com.tourya.api.models.TourGallery;
import com.tourya.api.models.request.TourGalleryRequest;
import com.tourya.api.models.responses.TourGalleryResponse;
import org.springframework.stereotype.Component;

@Component
public class TourGalleryMapper {

    public TourGallery toTourGallery(TourGalleryRequest request) {
        return TourGallery.builder()
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .orderIndex(request.getOrderIndex())
                .build();
    }

    public TourGalleryResponse toTourGalleryResponse(TourGallery entity) {
        return new TourGalleryResponse(
                entity.getId(),
                entity.getImageUrl(),
                entity.getDescription(),
                entity.getOrderIndex()
        );
    }
}

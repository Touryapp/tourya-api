package com.tourya.api.models.mapper;

import com.tourya.api.models.RequestProviderDocumentType;
import com.tourya.api.models.RequestProviderGallery;
import com.tourya.api.models.request.RequestProviderGalleryRequest;
import com.tourya.api.models.responses.RequestProviderGalleryResponse;
import org.springframework.stereotype.Component;

@Component
public class RequestProviderGalleryMapper {

    /**
     * Convierte un DTO de request en la entidad JPA.
     */
    public RequestProviderGallery toRequestProviderGallery(RequestProviderGalleryRequest request) {
        RequestProviderGallery entity = new RequestProviderGallery();
        entity.setImageUrl(request.getImageUrl());
        entity.setDescription(request.getDescription());
        entity.setOrderIndex(request.getOrderIndex());
        RequestProviderDocumentType docType = new RequestProviderDocumentType();
        docType.setId(request.getDocumentTypeId());
        entity.setDocumentType(docType);
        return entity;
    }

    /**
     * Convierte la entidad JPA en un DTO de respuesta para el frontend.
     */
    public RequestProviderGalleryResponse toRequestProviderGalleryResponse(RequestProviderGallery entity) {
        RequestProviderGalleryResponse resp = new RequestProviderGalleryResponse();
        resp.setId(entity.getId());
        resp.setImageUrl(entity.getImageUrl());
        resp.setDescription(entity.getDescription());
        resp.setOrderIndex(entity.getOrderIndex());
        if (entity.getDocumentType() != null) {
            resp.setDocumentTypeId(entity.getDocumentType().getId());
            resp.setDocumentTypeName(entity.getDocumentType().getName());
        }
        return resp;
    }
}

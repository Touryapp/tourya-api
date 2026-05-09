package com.tourya.api.models.mapper;

import com.tourya.api.models.TouryaService;
import com.tourya.api.models.responses.ServiceResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades Service a DTOs de respuesta.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Component
public class ServiceMapper {

    /**
     * Convierte una entidad Service a ServiceResponse.
     * 
     * @param service entidad Service
     * @return ServiceResponse
     */
    public ServiceResponse toResponse(TouryaService service) {
        if (service == null) {
            return null;
        }

        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .serviceTypeId(service.getServiceType() != null ? service.getServiceType().getId() : null)
                .serviceTypeName(service.getServiceType() != null ? service.getServiceType().getName() : null)
                .description(service.getDescription())
                .cancellationPolicy(service.getCancellationPolicy())
                .status(service.getStatus())
                .creationDate(service.getCreatedDate())
                .lastModifiedDate(service.getLastModifiedDate())
                .build();
    }
}

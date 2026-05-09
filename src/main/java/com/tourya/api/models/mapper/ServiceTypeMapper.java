package com.tourya.api.models.mapper;

import com.tourya.api.models.ServiceType;
import com.tourya.api.models.responses.ServiceTypeResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades ServiceType a DTOs de respuesta.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Component
public class ServiceTypeMapper {

    /**
     * Convierte una entidad ServiceType a ServiceTypeResponse.
     * 
     * @param serviceType entidad ServiceType
     * @return ServiceTypeResponse
     */
    public ServiceTypeResponse toResponse(ServiceType serviceType) {
        if (serviceType == null) {
            return null;
        }

        return ServiceTypeResponse.builder()
                .id(serviceType.getId())
                .name(serviceType.getName())
                .description(serviceType.getDescription())
                .status(serviceType.getStatus())
                .creationDate(serviceType.getCreatedDate())
                .lastModifiedDate(serviceType.getLastModifiedDate())
                .build();
    }
}



package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un servicio.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceResponse {

    private Integer id;
    private String name;
    private Integer serviceTypeId;
    private String serviceTypeName;
    private String description;
    private String cancellationPolicy;
    private String status;
    private LocalDateTime creationDate;
    private LocalDateTime lastModifiedDate;
}



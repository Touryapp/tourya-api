package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un tipo de servicio.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceTypeResponse {

    private Integer id;
    private String name;
    private String description;
    private String status;
    private LocalDateTime creationDate;
    private LocalDateTime lastModifiedDate;
}



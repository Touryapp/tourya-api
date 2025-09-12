package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la configuración de slot.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SlotRequest {

    @NotNull(message = "El ID del slot es requerido")
    private Long id;

    @NotNull(message = "La configuración de cantidad es requerida")
    @Valid
    private List<ConfigQuantityRequest> configQuantity;
}


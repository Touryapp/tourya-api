package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Request para crear reservas TEMPORAL (holds) por items del carrito.
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTemporalReservationHoldRequest {

    @NotEmpty(message = "Debe incluir al menos un item del carrito")
    private List<@Positive(message = "El ID del item debe ser positivo") Long> shoppingCartItemIds;

    @Valid
    @NotNull(message = "Los datos del responsable del servicio son obligatorios")
    private ServiceResponsibleRequest serviceResponsible;

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceResponsibleRequest {
        @NotNull(message = "El nombre del responsable es obligatorio")
        private String name;
        @NotNull(message = "El email del responsable es obligatorio")
        private String email;
        private String phone;
    }
}


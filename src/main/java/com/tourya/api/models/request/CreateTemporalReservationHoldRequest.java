package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Request para crear reservas TEMPORAL (holds) en checkout seguro.
 * Incluye responsable del servicio, hospedaje, lugar de procedencia y facturación electrónica.
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

    @Size(max = 255)
    private String accommodationName;

    private Double accommodationLatitude;

    private Double accommodationLongitude;

    private Integer originCountryId;

    private Integer originStateId;

    private Integer originCityId;

    private Boolean electronicBilling;

    @Size(max = 50)
    private String billingDocumentType;

    @Size(max = 50)
    private String billingDocumentNumber;

    @Email
    @Size(max = 255)
    private String billingEmail;

    @Size(max = 255)
    private String billingCustomerName;

    @Size(max = 30)
    private String billingPhone;

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

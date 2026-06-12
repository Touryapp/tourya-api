package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
 * Cada item puede tener su propio responsable del servicio; payer es común en el pago.
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTemporalReservationHoldRequest {

    /**
     * Preferido: un responsable por item del carrito.
     */
    @Valid
    private List<HoldItemRequest> items;

    /**
     * @deprecated Usar {@link #items}. Se mantiene por compatibilidad.
     */
    private List<@Positive(message = "El ID del item debe ser positivo") Long> shoppingCartItemIds;

    /**
     * @deprecated Usar {@link #items}. Se mantiene por compatibilidad (mismo responsable para todos).
     */
    @Valid
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

    @AssertTrue(message = "Debe enviar items (con responsable por item) o shoppingCartItemIds con serviceResponsible")
    public boolean isHoldItemsValid() {
        if (items != null && !items.isEmpty()) {
            return true;
        }
        return shoppingCartItemIds != null
                && !shoppingCartItemIds.isEmpty()
                && serviceResponsible != null;
    }

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HoldItemRequest {
        @NotNull(message = "El ID del item del carrito es obligatorio")
        @Positive(message = "El ID del item debe ser positivo")
        private Long shoppingCartItemId;

        @Valid
        @NotNull(message = "Los datos del responsable del servicio son obligatorios")
        private ServiceResponsibleRequest serviceResponsible;
    }

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

package com.tourya.api.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateShoppingCartCheckoutRequest {

    @Size(max = 255)
    private String accommodationName;

    private Double accommodationLatitude;

    private Double accommodationLongitude;

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
}

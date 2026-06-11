package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para el carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCartResponse {

    private Long id;
    private Integer userId;
    private ShoppingCartStatusEnum status;
    private List<ShoppingCartItemResponse> items;
    private BigDecimal totalAmount;
    private BigDecimal providerTotalAmount;
    private LocalDateTime creationDate;
    private LocalDateTime lastModifiedDate;

    private String accommodationName;
    private Double accommodationLatitude;
    private Double accommodationLongitude;
    private Integer originCountryId;
    private Integer originStateId;
    private Integer originCityId;
    private Boolean electronicBilling;
    private String billingDocumentType;
    private String billingDocumentNumber;
    private String billingEmail;
    private String billingCustomerName;
    private String billingPhone;
}





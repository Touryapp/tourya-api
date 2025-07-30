package com.tourya.api.models.mapper.responses;

import com.tourya.api.constans.enums.AgePriceType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ShoppingCartItemResponse {
    private Long id;
    private Integer tourScheduleId;
    private String tourName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private AgePriceType ageType;
}

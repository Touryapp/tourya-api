package com.tourya.api.models.mapper.responses;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class ShoppingCartResponse {
    private Long id;
    private Integer userId;
    private ShoppingCartStatusEnum status;
    private List<ShoppingCartItemResponse> items;
    private BigDecimal totalAmount;
}

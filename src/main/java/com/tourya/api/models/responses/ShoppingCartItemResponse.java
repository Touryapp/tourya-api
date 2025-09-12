package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para un item del carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCartItemResponse {

    private Long id;
    private Integer productId;
    private String productType;
    private Integer serviceId;
    private String serviceName;
    private String serviceType;
    private LocalDate scheduleDate;
    private Integer tourScheduleId;
    private String tourName;
    private Integer slotId;
    private BigDecimal totalPrice;
    private ShoppingCartStatusEnum status;
    private List<ShoppingCartItemDetailResponse> details;
}


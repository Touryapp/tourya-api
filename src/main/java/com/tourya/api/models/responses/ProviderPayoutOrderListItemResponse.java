package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
public class ProviderPayoutOrderListItemResponse {
    private Long id;
    private Integer providerId;
    private OffsetDateTime createdAt;
    private LocalDate payDate;
    private ProviderPayoutOrderStatusEnum status;
    private BigDecimal amountTotal;
    private Integer reservationsCount;
}


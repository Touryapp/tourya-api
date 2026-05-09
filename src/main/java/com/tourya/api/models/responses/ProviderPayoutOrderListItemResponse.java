package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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

    /** URL del comprobante subido por backoffice (último adjunto si hay varios); null si aún no hay archivo. */
    @Schema(description = "URL pública del comprobante de pago (si existe); null si no se ha subido")
    private String proofUrl;
}


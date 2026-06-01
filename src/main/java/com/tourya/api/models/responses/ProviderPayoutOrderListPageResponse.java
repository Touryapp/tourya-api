package com.tourya.api.models.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Schema(description = "Listado de órdenes de pago con totales agregados por estado")
public class ProviderPayoutOrderListPageResponse {
  @Schema(description = "Órdenes que coinciden con los filtros")
  private List<ProviderPayoutOrderListItemResponse> orders;
  @Schema(description = "Suma de amountTotal en estado PAID")
  private BigDecimal paidTotal;
  @Schema(description = "Suma de amountTotal en estado PENDING")
  private BigDecimal pendingTotal;
  @Schema(description = "Suma de amountTotal en estado CANCELED")
  private BigDecimal canceledTotal;
  @Schema(description = "paidTotal + pendingTotal")
  private BigDecimal totalIncome;
}

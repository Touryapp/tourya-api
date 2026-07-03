package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ProviderPayoutOrderDetailsResponse {
    private Long id;
    private Integer providerId;
    private OffsetDateTime createdAt;
    private LocalDate payDate;
    @Schema(description = "Estado del **lote/transferencia** al proveedor (no confundir con payoutStatus de cada reserva)")
    private ProviderPayoutOrderStatusEnum status;
    private BigDecimal amountTotal;

    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @Builder.Default
    private List<Item> reservations = new ArrayList<>();

    @Data
    @Builder
    public static class Attachment {
        private Long id;
        private String fileUrl;
        private OffsetDateTime createdAt;
    }

    @Data
    @Builder
    public static class Item {
        private Long reservationId;
        private Long accountPayableId;
        private BigDecimal amount;
        private LocalDate payoutAvailableDate;
        @Schema(description = "Pago al proveedor **por esta reserva** (PENDING | PAID). Pasa a PAID cuando la orden se marca pagada.")
        private String payoutStatus;
        private LocalDate scheduleDate;
        private LocalTime slotTimeStart;
        private LocalTime slotTimeEnd;
        private Long totalTourists;
        private LocalDateTime reservationCreatedDate;
        private LocalDate maxCancellationDate;
        private LocalDate maxReschedulingDate;
        private Boolean allowsRainRefund;
    }
}


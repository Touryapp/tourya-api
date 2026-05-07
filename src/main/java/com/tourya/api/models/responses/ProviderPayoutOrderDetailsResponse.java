package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        private String payoutStatus;
    }
}


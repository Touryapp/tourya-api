package com.tourya.api.models.request;

import com.tourya.api.constans.enums.AddressTypeEnum;
import com.tourya.api.constans.enums.CancellationPolicyTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourCancellationPolicyRequest {
    private Integer id;
    private String observations;
    private boolean allowsRainRefund = Boolean.TRUE;
    private boolean allowsRescheduling = Boolean.TRUE;
    @NotNull(message = "cancellationPolicyType is mandatory")
    private CancellationPolicyTypeEnum cancellationPolicyType;
}

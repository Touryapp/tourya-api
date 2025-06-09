package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.CancellationPolicyTypeEnum;
import lombok.Data;

@Data
public class TourCancellationPolicyResponse {
    private Integer id;
    private String observations;
    private boolean allowsRainRefund;
    private boolean allowsRescheduling;
    private CancellationPolicyTypeEnum cancellationPolicyType;
}

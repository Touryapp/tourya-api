package com.tourya.api.models.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransferCreditRequest {

    @NotNull
    @Positive
    private Integer targetUserId;
}

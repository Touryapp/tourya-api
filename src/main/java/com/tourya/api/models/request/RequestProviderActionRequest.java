package com.tourya.api.models.request;

import lombok.Data;

@Data
public class RequestProviderActionRequest {
    private String declinedReason;
    private String incompleteReason;
}

package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestProviderDocumentTypeResponse {
    private Integer id;
    private String name;
    private Boolean mandatory;
}

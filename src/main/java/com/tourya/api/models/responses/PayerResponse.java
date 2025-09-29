package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PayerResponse {
    private String name;
    private String email;
    private Integer id;
    private String phone;
    private String documentType;
    private String documentNumber;
}
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
public class ReservationItemResponse {
    private Long id;
    private Long shoppingCartItemId;
    private String serviceResponsibleName;
    private String serviceResponsibleEmail;
    private String serviceResponsiblePhone;
}
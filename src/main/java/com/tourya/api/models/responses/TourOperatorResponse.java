package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Operario principal del tour por parte del operador turístico (agencia).
 * Distinto de {@link ServiceResponsibleResponse}, que es quien asiste al tour.
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TourOperatorResponse {

    private Integer providerUserId;
    private String name;
    private String email;
    private Long phone;
}

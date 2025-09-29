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
public class ServiceResponsibleResponse {
    private String name;
    private String email;
    private Long phone; // Cambiado a Long para coincidir con el ejemplo
}
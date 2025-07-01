package com.tourya.api.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourScheduleConfigPriceDto {
    private Integer id; // Agregado para permitir actualizaciones (identificar precios existentes)
    @NotBlank(message = "El tipo de edad no puede estar vacío")
    @Size(max = 20, message = "El tipo de edad no puede exceder los 20 caracteres") // AÑADIDA ESTA VALIDACIÓN
    private String ageType;
    @NotNull(message = "La edad mínima no puede ser nula")
    @PositiveOrZero(message = "La edad mínima debe ser un número no negativo")
    private Integer minAge;
    @NotNull(message = "La edad máxima no puede ser nula")
    @PositiveOrZero(message = "La edad máxima debe ser un número no negativo")
    private Integer maxAge;
    @NotNull(message = "El precio no puede ser nulo")
    @PositiveOrZero(message = "El precio debe ser un número no negativo")
    private BigDecimal price;
}

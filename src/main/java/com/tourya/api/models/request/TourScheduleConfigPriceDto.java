package com.tourya.api.models.request;

import com.tourya.api.constans.enums.AgePriceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourScheduleConfigPriceDto {
    private Integer id; // Agregado para permitir actualizaciones (identificar precios existentes)
    @NotNull(message = "El tipo de edad no puede ser nulo")
    private AgePriceType ageType;
    @NotNull(message = "La edad mínima no puede ser nula")
    @PositiveOrZero(message = "La edad mínima debe ser un número no negativo")
    private Integer minAge;
    @NotNull(message = "La edad máxima no puede ser nula")
    @PositiveOrZero(message = "La edad máxima debe ser un número no negativo")
    private Integer maxAge;
    @NotNull(message = "El precio no puede ser nulo")
    @PositiveOrZero(message = "El precio debe ser un número no negativo")
    private BigDecimal price;
    @PositiveOrZero(message = "El precio del proveedor debe ser un número no negativo")
    private BigDecimal providerPrice;
}

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

    @NotNull(message = "El precio no puede ser nulo")
    @PositiveOrZero(message = "El precio debe ser un número no negativo")
    private BigDecimal price;
    @PositiveOrZero(message = "El precio del proveedor debe ser un número no negativo")
    private BigDecimal providerPrice;
}

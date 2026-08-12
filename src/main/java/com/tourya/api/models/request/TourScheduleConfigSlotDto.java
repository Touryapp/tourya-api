package com.tourya.api.models.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tourya.api.config.EmptyStringAsNullIntegerDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class TourScheduleConfigSlotDto {
    @JsonDeserialize(using = EmptyStringAsNullIntegerDeserializer.class)
    private Integer id; // Agregado para permitir actualizaciones (identificar slots existentes)
    @NotNull(message = "La hora de inicio del slot no puede ser nula")
    private LocalTime startTime;
    @NotNull(message = "La hora de fin del slot no puede ser nula")
    private LocalTime endTime;
    @NotNull(message = "La capacidad del slot no puede ser nula")
    private Integer capacity;

    // TC-019 (#231) bug 4: porcentaje Tourya del slot en puntos porcentuales (0-100).
    // Nullable: si no viene, se preserva la logica existente (tour default para slots nuevos,
    // valor de BD para existentes). Solo BACKOFFICE puede setearlo (RN-015); si un PROVIDER
    // lo envia, se ignora silenciosamente.
    @DecimalMin(value = "0", message = "slotPorcentajeTourya no puede ser negativo")
    @DecimalMax(value = "100", message = "slotPorcentajeTourya no puede superar 100")
    private BigDecimal slotPorcentajeTourya;

    @Valid // Habilita la validación anidada de los precios
    @NotEmpty(message = "Cada slot debe tener al menos un precio asociado")
    private List<TourScheduleConfigPriceDto> prices; // Precios asociados a este slot
}

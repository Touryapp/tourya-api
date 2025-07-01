package com.tourya.api.models.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TourScheduleConfigRequest {

    @Size(max = 255, message = "Label cannot exceed 255 characters") // Ajusta el tamaño si `text` es muy largo
    private String label; // Corresponde a 'label' en la tabla

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate; // Corresponde a 'start_date' en la tabla

    @NotNull(message = "End date is mandatory")
    private LocalDate endDate; // Corresponde a 'end_date' en la tabla

    // daysOfWeek: Podría ser una lista de Strings (ej. "MON", "TUE")
    // Considera validación si solo ciertos días son válidos
    private List<String> daysOfWeek; // Corresponde a 'days_of_week' en la tabla

    private Boolean isUnlimitedCapacity; // Corresponde a 'is_unlimited_capacity' en la tabla
}

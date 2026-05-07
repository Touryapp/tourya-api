package com.tourya.api.models.request;

import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request para crear una nueva reseña.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "El ID de la reserva es obligatorio")
    @Positive(message = "El ID de la reserva debe ser positivo")
    private Long reservationId;

    @NotNull(message = "La calificación es obligatoria")
    @DecimalMin(value = "1.00", inclusive = true, message = "La calificación debe ser entre 1 y 5")
    @DecimalMax(value = "5.00", inclusive = true, message = "La calificación debe ser entre 1 y 5")
    private BigDecimal rating;

    @Valid
    private TranslatedField comment;

    private LocalDate date;
}


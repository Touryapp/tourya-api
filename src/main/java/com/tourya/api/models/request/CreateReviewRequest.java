package com.tourya.api.models.request;

import com.tourya.api.models.TranslatedField;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
        name = "CreateReviewRequest",
        description = "Payload JSON enviado en la parte multipart `reviewData`. "
                + "Solo se admite **un** motivo opcional: `reasonId` (entero del catálogo, ver endpoint de motivos). "
                + "Si `reasonId` viene informado, el backend infiere POSITIVE/NEGATIVE según la calificación."
)
public class CreateReviewRequest {

    @NotNull(message = "El ID de la reserva es obligatorio")
    @Positive(message = "El ID de la reserva debe ser positivo")
    @Schema(description = "ID de la reserva (`reservationId` de la reserva)", example = "214")
    private Long reservationId;

    @NotNull(message = "La calificación es obligatoria")
    @DecimalMin(value = "1.00", inclusive = true, message = "La calificación debe ser entre 1 y 5")
    @DecimalMax(value = "5.00", inclusive = true, message = "La calificación debe ser entre 1 y 5")
    @Schema(description = "Calificación de 1 a 5", example = "5.0")
    private BigDecimal rating;

    @Valid
    @Schema(description = "Comentario multilenguaje (es obligatorio en `es`)")
    private TranslatedField comment;

    @Min(value = 1, message = "El motivo debe ser entre 1 y 7")
    @Max(value = 7, message = "El motivo debe ser entre 1 y 7")
    @Schema(
            description = "Opcional. **Un solo** ID de motivo del catálogo (1–7). Omitir si no aplica.",
            example = "3",
            nullable = true
    )
    private Integer reasonId;

    @Schema(description = "Fecha de la reseña (opcional; por defecto hoy)", example = "2026-05-08", nullable = true)
    private LocalDate date;
}


package com.tourya.api.models.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourRequest {

    @NotEmpty(message = "Name is mandatory")
    @NotNull(message = "Name is mandatory")
    private String name;

    @NotEmpty(message = "Description is mandatory")
    @NotNull(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Tour Category ID is mandatory")
    private Integer tourCategoryId;

    private String duration;

    private Integer maxPeople = 0; // Valor por defecto 0

    private Integer highlight = 0; // Valor por defecto 0

    @NotNull(message = "Price is mandatory")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
    // Puedes añadir @DecimalMax si hay un precio máximo
    private BigDecimal price;

    @NotNull(message = "Minimum age is mandatory")
    @Min(value = 0, message = "Minimum age cannot be negative") // La edad mínima no puede ser negativa
    @Max(value = 99, message = "Minimum age seems too high") // Un límite superior razonable
    private Integer minAge;

    @NotNull(message = "Rating is mandatory")
    @DecimalMin(value = "0.00", inclusive = true, message = "Rating must be at least 0.00")
    @DecimalMax(value = "5.00", inclusive = true, message = "Rating must be at most 5.00") // Asumiendo una escala de 0 a 5
    private BigDecimal rating;
}

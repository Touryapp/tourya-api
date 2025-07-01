package com.tourya.api.models.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TourScheduleConfigPriceRequest {
    @NotNull(message = "Slot ID cannot be null")
    private Integer slotId;

    @NotBlank(message = "Age type cannot be blank")
    private String ageType;

    @NotNull(message = "Min age cannot be null")
    @Min(value = 0, message = "Min age must be at least 0")
    private Integer minAge;

    @NotNull(message = "Max age cannot be null")
    @Min(value = 0, message = "Max age must be at least 0")
    private Integer maxAge;

    @NotNull(message = "Price cannot be null")
    @Min(value = 0, message = "Price must be a positive value")
    private BigDecimal price;

}

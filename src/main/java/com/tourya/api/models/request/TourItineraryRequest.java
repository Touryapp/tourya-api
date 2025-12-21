package com.tourya.api.models.request;

import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TourItineraryRequest {
    private Integer id;
    @Valid
    @NotNull(message = "Title is mandatory")
    private TranslatedField title;

    @NotNull(message = "Day is mandatory")
    @Min(value = 1, message = "Day must be greater than or equal to 1")
    private Integer day;

    @NotNull(message = "Time is mandatory")
    private LocalTime time;

    @Valid
    @NotNull(message = "Description is mandatory")
    private TranslatedField description;
}


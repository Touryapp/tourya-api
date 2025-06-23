package com.tourya.api.models.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TourItineraryRequest {
    private Integer id;
    @NotEmpty(message = "Title is mandatory")
    private String title;

    @NotNull(message = "Day is mandatory")
    @Min(value = 1, message = "Day must be greater than or equal to 1")
    private Integer day;

    @NotNull(message = "Time is mandatory")
    private LocalTime time;

    @NotEmpty(message = "Description is mandatory")
    private String description;
}


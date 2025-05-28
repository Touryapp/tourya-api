package com.tourya.api.models.resquest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class TourFullDataRequest {
    @NotEmpty(message = "name is mandatory")
    @NotNull(message = "name is mandatory")
    private String name;

    @NotEmpty(message = "description is mandatory")
    @NotNull(message = "description is mandatory")
    private String description;

    @NotNull(message = "Tour Category ID is mandatory")
    private Integer tourCategoryId;

    private String duration;

    private Integer maxPeople = 0; // Valor por defecto 0

    private Integer highlight = 0; // Valor por defecto 0

    @Valid
    @NotNull(message = "locations is mandatory")
    private List<TourAddressRequest> locations = new ArrayList<>();

    @Valid
    @NotNull(message = "mainAttractions is mandatory")
    private List<TourMainAttractionRequest> mainAttractions = new ArrayList<>();

    @Valid
    @NotNull(message = "includes is mandatory")
    private List<TourIncludesExcludesRequest>  includes = new ArrayList<>();

    @Valid
    @NotNull(message = "excludes is mandatory")
    private List<TourIncludesExcludesRequest>  excludes = new ArrayList<>();
}

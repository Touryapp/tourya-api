package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Data
public class TourFullDataRequest {
    private Integer id;
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

    @Valid
    @NotNull(message = "faq is mandatory")
    private List<TourFaqRequest>  faq = new ArrayList<>();

    @Valid
    @NotNull(message = "itinerary is mandatory")
    private List<TourItineraryRequest>  itineraries = new ArrayList<>();

    @Valid
    @NotNull(message = "cancellationPolicies is mandatory")
    private List<TourCancellationPolicyRequest>  cancellationPolicies = new ArrayList<>();

    @Valid
    @NotNull(message = "gallery is mandatory")
    private List<TourGalleryRequest>  galleries = new ArrayList<>();

}

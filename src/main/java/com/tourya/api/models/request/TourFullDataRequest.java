package com.tourya.api.models.request;

import com.tourya.api.models.TranslatedField;
import com.tourya.api.constans.enums.PriceTypeEnum;
import com.tourya.api.constans.enums.TourDurationEnum;
import com.tourya.api.constans.enums.TourSubCategoryEnum;
import com.tourya.api.constans.enums.TourTimeOfDayEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Valid
    @NotNull(message = "name is mandatory")
    @Schema(description = "Nombre del tour en formato multiidioma", example = "{\"es\":\"Tour bahia diurno\",\"en\":\"Bay tour\",\"pt\":\"Passeio pela baia\"}")
    private TranslatedField name;

    @Valid
    @Schema(description = "Descripción del tour en formato multiidioma", example = "{\"es\":\"Descripcion del tour\",\"en\":\"Tour description\",\"pt\":\"Descricao do tour\"}")
    private TranslatedField description;

    @NotNull(message = "Tour Category ID is mandatory")
    @Schema(description = "ID de la categoría del tour", example = "1")
    private Integer tourCategoryId;

    private String duration;

    private Integer maxPeople = 0; // Valor por defecto 0

    @NotNull(message = "priceType is mandatory")
    @Schema(description = "Tipo de precio del tour", example = "individual", allowableValues = {"individual", "grupo"})
    private PriceTypeEnum priceType;

    @NotNull(message = "isUnlimitedCapacity is mandatory")
    private Boolean isUnlimitedCapacity;

    @NotNull(message = "subCategory is mandatory")
    @Schema(description = "Código de la subcategoría del tour", example = "tour_bahia_diurno")
    private TourSubCategoryEnum subCategory;

    @NotNull(message = "durationEnum is mandatory")
    @Schema(description = "Duración categorizada del tour", example = "1_a_2_horas")
    private TourDurationEnum durationEnum;

    @NotEmpty(message = "timeOfDay is mandatory")
    @Schema(description = "Momentos del día en que aplica el tour", example = "[\"manana\"]")
    private List<TourTimeOfDayEnum> timeOfDay;

    private Integer highlight = 0; // Valor por defecto 0

    @NotNull(message = "Minimum age is mandatory")
    @Min(value = 0, message = "Minimum age cannot be negative") // La edad mínima no puede ser negativa
    @Max(value = 99, message = "Minimum age seems too high") // Un límite superior razonable
    private Integer minAge;

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

    @Schema(description = "IDs de tags seleccionados para el tour", example = "[61,66,62]")
    private List<Integer> tagIds;

    private ModifiedArrayListRequest  modifiedArrayList;
}

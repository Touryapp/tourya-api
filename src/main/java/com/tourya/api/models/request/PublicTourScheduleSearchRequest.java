package com.tourya.api.models.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Filtros para la búsqueda pública de tours y horarios")
public class PublicTourScheduleSearchRequest {
    @Schema(description = "ID de la categoría del tour", example = "1")
    private Integer categoryId;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "IDs de categorías para multiselección", example = "[1,4]")
    private List<Integer> categoryIds;

    @Schema(description = "ID específico del tour", example = "43")
    private Integer tourId;

    @Schema(description = "Código de la subcategoría", example = "tour_bahia_diurno")
    private String subCategory;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "Códigos de subcategorías para multiselección", example = "[\"tour_bahia_diurno\",\"jet_ski\"]")
    private List<String> subCategories;

    @Schema(description = "Duración categorizada del tour", example = "1_a_2_horas")
    private String durationEnum;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "Duraciones para multiselección", example = "[\"1_a_2_horas\",\"2_a_4_horas\"]")
    private List<String> durationEnums;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "Momentos del día para multiselección", example = "[\"manana\",\"tarde\"]")
    private List<String> timeOfDay;

    @Schema(description = "Fecha inicial de búsqueda", example = "2026-04-01")
    private LocalDate startDate;

    @Schema(description = "Fecha final de búsqueda", example = "2026-04-30")
    private LocalDate endDate;

    @Schema(description = "Precio mínimo filtro ADULT", example = "50000")
    private BigDecimal minPrice;

    @Schema(description = "Precio máximo filtro ADULT", example = "200000")
    private BigDecimal maxPrice;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "Tags a buscar por nombre", example = "[\"Vida Marina\",\"Adrenalina\"]")
    private List<String> tags;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Schema(description = "IDs de tags para multiselección", example = "[2,9,15]")
    private List<Integer> tagIds;

    @Schema(description = "Texto libre para buscar por nombre o descripción", example = "bahia")
    private String textSearch;

    @Schema(description = "Idioma de búsqueda sobre campos traducidos", example = "es", defaultValue = "es")
    private String language;

    @Schema(description = "Cantidad requerida de cupos/unidades. También acepta participants o quantity en backend.", example = "4")
    private Integer requestedUnits;

    @Schema(description = "Filtrar precios por tipo de edad", example = "ADULT")
    private String ageType;

    @Schema(description = "ID del estado del tour", example = "1")
    private Integer stateId;

    @Schema(description = "ID de la ciudad del tour", example = "1")
    private Integer cityId;

    @Schema(description = "ID del estado del proveedor", example = "1")
    private Integer providerStateId;

    @Schema(description = "ID de la ciudad del proveedor", example = "1")
    private Integer providerCityId;
}

package com.tourya.api.agents.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * IA-07: request de {@code POST /agents/operator-support/suggest-tour-content}.
 *
 * <p>El operador puede llamar al agente:</p>
 * <ul>
 *   <li>Desde el wizard mientras crea un tour ({@code tourId=null}, {@code draft}
 *       trae lo que ya cargo).</li>
 *   <li>Desde la vista de edicion de un tour existente ({@code tourId} presente,
 *       {@code draft} opcional con overrides).</li>
 * </ul>
 */
@Data
public class SuggestTourContentRequest {

    @Schema(description = "ID del tour a mejorar (opcional — para editar tours existentes).",
            example = "42")
    private Integer tourId;

    @Valid
    @NotNull(message = "draft es obligatorio")
    @Schema(description = "Datos parciales del tour desde el wizard.", required = true)
    private TourDraft draft;
}

package com.tourya.api.agents.operator;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * IA-07: draft parcial del wizard de creacion de tour que el frontend envia
 * al agente Operator Support antes de guardar. Todos los campos son opcionales
 * — el agente trabaja con lo que tenga y pide contexto adicional en la
 * respuesta si le faltan piezas criticas.
 *
 * @param name                Nombre en espanol (draft parcial).
 * @param categoryId          ID de {@code tour_category}.
 * @param subcategory         Slug de {@code tour_subcategory_enum} (ej. "SNORKEL_TOUR").
 * @param durationMinutes     Duracion aproximada en minutos.
 * @param minAge              Edad minima recomendada.
 * @param priceType           {@code PER_PERSON} / {@code PER_GROUP} / etc.
 * @param isUnlimitedCapacity Si el tour no tiene tope de plazas.
 * @param currentDescription  Descripcion actual (si esta editando un tour existente).
 * @param maxPeople           Cupo maximo (cuando aplica).
 */
@Schema(description = "Draft del tour del wizard — todos los campos opcionales")
public record TourDraft(
        String name,
        Integer categoryId,
        String subcategory,
        Integer durationMinutes,
        Integer minAge,
        String priceType,
        Boolean isUnlimitedCapacity,
        String currentDescription,
        Integer maxPeople
) {}

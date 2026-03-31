package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tourya.api.exceptions.UnknownEnumValueException;
import lombok.AllArgsConstructor;

/**
 * Subcategorías del tour (keys). Relacionadas lógicamente con la categoría principal.
 */
@AllArgsConstructor
public enum TourSubCategoryEnum {
    // 🛥️ Acuático
    PASEO_AL_CAYO("paseo_al_cayo"),
    TOUR_BAHIA_DIURNO("tour_bahia_diurno"),
    PONTON("ponton"),
    YATE_DE_LUJO("yate_de_lujo"),
    BAR_EN_EL_AGUA("bar_en_el_agua"),
    SEMI_SUBMARINO("semi_submarino"),
    SNORKELING("snorkeling"),

    // 🤿 Deportes
    BUCEO("buceo"),
    SNUBA("snuba"),
    WINDSURF("windsurf"),
    ESQUI_ACUATICO("esqui_acuatico"),
    WAKEBOARD("wakeboard"),
    FLY_BOARD("fly_board"),
    KAYAK("kayak"),

    // 🏝️ Terrestre
    VUELTA_A_LA_ISLA_CITY_TOUR("vuelta_a_la_isla_city_tour"),

    // 🪂 Aventura
    PARASAIL("parasail"),
    JET_SKI("jet_ski"),

    // 🌙 Nocturno
    FIESTA_NOCHE_BLANCA("fiesta_noche_blanca"),

    // 🗿 Cultural / Experiencias
    PADDLE_BOARD("paddle_board"),
    AQUANAUTAS("aquanautas"),
    PICNIC("picnic"),
    COCINA_LOCAL("cocina_local"),

    // 🛵 Alquiler de Transporte
    MOTO("moto"),
    BICICLETA("bicicleta"),
    CARRO_PLAYERO("carro_playero");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TourSubCategoryEnum of(String value) {
        for (TourSubCategoryEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new UnknownEnumValueException("TourSubCategoryEnum: unknown value: " + value);
    }
}


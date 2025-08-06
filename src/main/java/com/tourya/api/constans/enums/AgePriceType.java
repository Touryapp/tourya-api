package com.tourya.api.constans.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT) // 🔹 Esto hace que se serialice como objeto
public enum AgePriceType {
    ADULT("ADULT"),
    CHILD("CHILD"),
    INFANT("INFANT");

    private final String name;
}

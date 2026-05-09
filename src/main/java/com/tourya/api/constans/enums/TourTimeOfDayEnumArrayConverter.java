package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * Convierte una lista de {@link TourTimeOfDayEnum} a un arreglo de Strings para columna PG enum[].
 */
@Converter
public class TourTimeOfDayEnumArrayConverter implements AttributeConverter<List<TourTimeOfDayEnum>, String[]> {

    @Override
    public String[] convertToDatabaseColumn(List<TourTimeOfDayEnum> attribute) {
        if (attribute == null) return null;
        return attribute.stream().map(TourTimeOfDayEnum::getValue).toArray(String[]::new);
    }

    @Override
    public List<TourTimeOfDayEnum> convertToEntityAttribute(String[] dbData) {
        if (dbData == null) return null;
        return Arrays.stream(dbData).map(TourTimeOfDayEnum::of).toList();
    }
}


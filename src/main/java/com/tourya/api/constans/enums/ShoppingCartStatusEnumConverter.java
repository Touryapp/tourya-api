package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class ShoppingCartStatusEnumConverter implements AttributeConverter<ShoppingCartStatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(ShoppingCartStatusEnum status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public ShoppingCartStatusEnum convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        return Stream.of(ShoppingCartStatusEnum.values())
                .filter(c -> c.name().equals(code))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}

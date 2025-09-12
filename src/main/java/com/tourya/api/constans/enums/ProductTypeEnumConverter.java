package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter para el enum ProductTypeEnum para persistencia en base de datos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Converter(autoApply = true)
public class ProductTypeEnumConverter implements AttributeConverter<ProductTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(ProductTypeEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ProductTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return ProductTypeEnum.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para ProductTypeEnum: " + dbData);
        }
    }
}



package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter para el enum OrderStatusEnum para persistencia en base de datos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Converter(autoApply = true)
public class OrderStatusEnumConverter implements AttributeConverter<OrderStatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatusEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public OrderStatusEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OrderStatusEnum.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para OrderStatusEnum: " + dbData);
        }
    }
}



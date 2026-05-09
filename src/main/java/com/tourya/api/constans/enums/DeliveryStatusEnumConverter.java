package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter para el enum DeliveryStatusEnum para persistencia en base de datos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Converter(autoApply = true)
public class DeliveryStatusEnumConverter implements AttributeConverter<DeliveryStatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(DeliveryStatusEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public DeliveryStatusEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return DeliveryStatusEnum.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para DeliveryStatusEnum: " + dbData);
        }
    }
}



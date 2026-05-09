package com.tourya.api.constans.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter para el enum PaymentMethodTypeEnum.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Converter(autoApply = true)
public class PaymentMethodTypeEnumConverter implements AttributeConverter<PaymentMethodTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethodTypeEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public PaymentMethodTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return PaymentMethodTypeEnum.fromValue(dbData);
    }
}

package com.tourya.api.constans.enums;

import jakarta.persistence.Convert;

@Convert
public class IncludeExcludeTypeEnumCoverter {
    public String convertToDatabaseColumn(IncludeExcludeTypeEnum value) {
        if ( value == null ) {
            return null;
        }

        return value.getValue();
    }

    public IncludeExcludeTypeEnum convertToEntityAttribute(String value) {
        if ( value == null ) {
            return null;
        }

        return IncludeExcludeTypeEnum.of(value);
    }
}


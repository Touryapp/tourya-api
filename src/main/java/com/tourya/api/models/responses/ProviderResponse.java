package com.tourya.api.models.responses;


import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.constans.enums.ProviderDocumentTypeEnum;
import com.tourya.api.constans.enums.ProviderDocumentTypeEnumConverter;
import com.tourya.api.constans.enums.ProviderServiceTypeEnum;
import com.tourya.api.constans.enums.ProviderServiceTypeEnumConverter;
import jakarta.persistence.Convert;
import lombok.Data;

@Data
public class ProviderResponse {
    private Integer id;

    private String name;

    private String documentNumber;

    @Convert(converter = ProviderDocumentTypeEnumConverter.class)
    private ProviderDocumentTypeEnum documentType;

    @Convert(converter = ProviderServiceTypeEnumConverter.class)
    private ProviderServiceTypeEnum serviceType;

    private CountryResponse country;

    private CityLightResponse city;

    private StateLightResponse state;

    private String department;

    private String address;

    private String phone;

    private ProviderStatusEnum status;
}

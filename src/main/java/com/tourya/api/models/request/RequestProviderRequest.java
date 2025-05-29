package com.tourya.api.models.request;


import com.tourya.api.constans.enums.ProviderDocumentTypeEnum;
import com.tourya.api.constans.enums.ProviderDocumentTypeEnumConverter;
import com.tourya.api.constans.enums.ProviderServiceTypeEnum;
import com.tourya.api.constans.enums.ProviderServiceTypeEnumConverter;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Data
public class RequestProviderRequest {
    @NotEmpty(message = "name is mandatory")
    @NotNull(message = "name is mandatory")
    private String name;

    @NotEmpty(message = "documentNumber is mandatory")
    @NotNull(message = "documentNumber is mandatory")
    private String documentNumber;

    @NotNull(message = "documentType is mandatory")
    @Convert(converter = ProviderDocumentTypeEnumConverter.class)
    private ProviderDocumentTypeEnum documentType;

    @NotNull(message = "serviceType is mandatory")
    @Convert(converter = ProviderServiceTypeEnumConverter.class)
    private ProviderServiceTypeEnum serviceType;

    @NotNull(message = "countryId is mandatory")
    private Integer countryId;

    @NotNull(message = "stateId is mandatory")
    private Integer stateId;

    @NotNull(message = "cityId is mandatory")
    private Integer cityId;

    @NotEmpty(message = "department is mandatory")
    @NotNull(message = "department is mandatory")
    private String department;

    @NotEmpty(message = "address is mandatory")
    @NotNull(message = "address is mandatory")
    private String address;

    @NotEmpty(message = "phone is mandatory")
    @NotNull(message = "phone is mandatory")
    private String phone;
}

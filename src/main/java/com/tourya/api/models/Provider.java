package com.tourya.api.models;


import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.constans.enums.ProviderStatusEnumConverter;
import com.tourya.api.constans.enums.ProviderDocumentTypeEnum;
import com.tourya.api.constans.enums.ProviderDocumentTypeEnumConverter;
import com.tourya.api.constans.enums.ProviderServiceTypeEnum;
import com.tourya.api.constans.enums.ProviderServiceTypeEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "provider")
public class Provider extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Utiliza la generación de identidad de la base de datos (serial)
    @Column(name = "id")
    private Integer id;

    private String name;

    @Column(name = "document_number")
    private String documentNumber;

    @Convert(converter = ProviderDocumentTypeEnumConverter.class)
    @Column(name = "document_type")
    private ProviderDocumentTypeEnum documentType;

    @Convert(converter = ProviderServiceTypeEnumConverter.class)
    @Column(name = "service_type")
    private ProviderServiceTypeEnum serviceType;

    // Many-to-One relationship with Country
    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    // Many-to-One relationship with State
    @ManyToOne
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    // Many-to-One relationship with City
    @ManyToOne
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    private String department;

    private String address;

    private String phone;

    @Convert(converter = ProviderStatusEnumConverter.class)
    @Column(name = "status")
    private ProviderStatusEnum status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

package com.tourya.api.models;

import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.AddressTypeEnum;
import com.tourya.api.constans.enums.AddressTypeEnumConverter;
import com.tourya.api.constans.enums.ProveedorTipoDocumentoEnumConverter;
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
@Table(name = "tour_address")
public class TourAddress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // Many-to-One relationship with Tour
    @ManyToOne
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

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

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    private String address;

    @Convert(converter = AddressTypeEnumConverter.class)
    @Column(name = "address_type")
    private AddressTypeEnum addressType;

}

package com.tourya.api.models;


import com.tourya.api.common.BaseEntity;
import com.tourya.api.constans.enums.SolicitudStatusEnum;
import com.tourya.api.constans.enums.SolicitudStatusEnumConverter;
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
@Table(name = "solicitud")
public class Solicitud extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Utiliza la generación de identidad de la base de datos (serial)
    @Column(name = "id")
    private Integer id;

    @Convert(converter = SolicitudStatusEnumConverter.class)
    @Column(name = "status")
    private SolicitudStatusEnum status;
    @ManyToOne
    @JoinColumn(name = "proveedorId", nullable = false)
    private Proveedor proveedor;
}

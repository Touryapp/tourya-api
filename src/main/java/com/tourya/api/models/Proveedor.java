package com.tourya.api.models;


import com.tourya.api.common.BaseEntity;
import jakarta.persistence.Column;
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
@Table(name = "proveedor")
public class Proveedor extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Utiliza la generación de identidad de la base de datos (serial)
    @Column(name = "id")
    private Integer id;

    private String nombre;

    @Column(name = "numeroDocumento")
    private String numeroDocumento;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @Column(name = "tipo_servicio")
    private String tipoServicio;

    private String pais;

    private String departamento;

    private String ciudad;

    private String direccion;

    private String telefono;

    private String status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

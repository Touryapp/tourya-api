package com.tourya.api.models.responses;


import lombok.Data;

@Data
public class ProveedorResponse {
    private Integer id;

    private String nombre;

    private String numeroDocumento;

    private String tipoDocumento;

    private String tipoServicio;

    private String pais;

    private String departamento;

    private String ciudad;

    private String direccion;

    private String telefono;

    private String status;
}

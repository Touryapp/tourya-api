package com.tourya.api.models.resquest;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ProveedorRequest {
    @NotEmpty(message = "Nombre is mandatory")
    @NotNull(message = "Nombre is mandatory")
    private String nombre;

    @NotEmpty(message = "Numero Documento is mandatory")
    @NotNull(message = "Numero Documento is mandatory")
    private String numeroDocumento;

    @NotEmpty(message = "Tipo Documento is mandatory")
    @NotNull(message = "Tipo Documento is mandatory")
    private String tipoDocumento;

    @NotEmpty(message = "Pais is mandatory")
    @NotNull(message = "Pais is mandatory")
    private String pais;

    @NotEmpty(message = "Departamento is mandatory")
    @NotNull(message = "Departamento is mandatory")
    private String departamento;

    @NotEmpty(message = "Ciudad is mandatory")
    @NotNull(message = "Ciudad is mandatory")
    private String ciudad;

    @NotEmpty(message = "Direccion is mandatory")
    @NotNull(message = "Direccion is mandatory")
    private String direccion;

    @NotEmpty(message = "Nombre is mandatory")
    @NotNull(message = "Nombre is mandatory")
    private String telefono;

}

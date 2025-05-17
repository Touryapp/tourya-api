package com.tourya.api.models.resquest;


import com.tourya.api.constans.enums.ProveedorTipoDocumentoEnum;
import com.tourya.api.constans.enums.ProveedorTipoDocumentoEnumConverter;
import com.tourya.api.constans.enums.ProveedorTipoServicioEnum;
import com.tourya.api.constans.enums.ProveedorTipoServicioEnumConverte;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Data
public class SolicitudRequest {
    @NotEmpty(message = "Nombre is mandatory")
    @NotNull(message = "Nombre is mandatory")
    private String nombre;

    @NotEmpty(message = "Numero Documento is mandatory")
    @NotNull(message = "Numero Documento is mandatory")
    private String numeroDocumento;

    @Convert(converter = ProveedorTipoDocumentoEnumConverter.class)
    private ProveedorTipoDocumentoEnum tipoDocumento;

    @Convert(converter = ProveedorTipoServicioEnumConverte.class)
    private ProveedorTipoServicioEnum tipoServicio;

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

    @NotEmpty(message = "Telefono is mandatory")
    @NotNull(message = "Telefono is mandatory")
    private String telefono;

}

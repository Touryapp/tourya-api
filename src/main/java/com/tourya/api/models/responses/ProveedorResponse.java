package com.tourya.api.models.responses;


import com.tourya.api.constans.enums.ProveedorStatusEnum;
import com.tourya.api.constans.enums.ProveedorTipoDocumentoEnum;
import com.tourya.api.constans.enums.ProveedorTipoDocumentoEnumConverter;
import com.tourya.api.constans.enums.ProveedorTipoServicioEnum;
import com.tourya.api.constans.enums.ProveedorTipoServicioEnumConverte;
import jakarta.persistence.Convert;
import lombok.Data;

@Data
public class ProveedorResponse {
    private Integer id;

    private String nombre;

    private String numeroDocumento;

    @Convert(converter = ProveedorTipoDocumentoEnumConverter.class)
    private ProveedorTipoDocumentoEnum tipoDocumento;

    @Convert(converter = ProveedorTipoServicioEnumConverte.class)
    private ProveedorTipoServicioEnum tipoServicio;

    private String pais;

    private String departamento;

    private String ciudad;

    private String direccion;

    private String telefono;

    private ProveedorStatusEnum status;
}

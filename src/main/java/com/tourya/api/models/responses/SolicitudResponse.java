package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class SolicitudResponse {
    private Integer id;
    private String status;
    private ProveedorResponse proveedor;
}

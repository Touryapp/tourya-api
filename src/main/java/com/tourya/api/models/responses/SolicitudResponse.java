package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.SolicitudStatusEnum;
import lombok.Data;

@Data
public class SolicitudResponse {
    private Integer id;
    private SolicitudStatusEnum status;
    private ProveedorResponse proveedor;
}

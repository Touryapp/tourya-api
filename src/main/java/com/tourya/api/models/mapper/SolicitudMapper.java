package com.tourya.api.models.mapper;

import com.tourya.api.models.Solicitud;
import com.tourya.api.models.responses.SolicitudResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SolicitudMapper {
    private final ProveedorMapper proveedorMapper;

    public SolicitudResponse toSolicitudResponse(Solicitud solicitud) {
        SolicitudResponse solicitudResponse = new SolicitudResponse();
        solicitudResponse.setId(solicitud.getId());
        solicitudResponse.setStatus(solicitud.getStatus());
        if(solicitud.getProveedor() != null) {
            solicitudResponse.setProveedor(proveedorMapper.toProveedorResponse(solicitud.getProveedor()));
        }
        return solicitudResponse;
    }
}

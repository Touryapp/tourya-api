package com.tourya.api.models.mapper;


import com.tourya.api.models.Proveedor;
import com.tourya.api.models.responses.ProveedorResponse;
import com.tourya.api.models.resquest.SolicitudRequest;
import org.springframework.stereotype.Service;


@Service
public class ProveedorMapper {

    public Proveedor toProveedor(SolicitudRequest request) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(request.getNombre());
        proveedor.setNumeroDocumento(request.getNumeroDocumento());
        proveedor.setTipoDocumento(request.getTipoDocumento());
        proveedor.setTipoServicio(request.getTipoServicio());
        proveedor.setCiudad(request.getCiudad());
        proveedor.setPais(request.getPais());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDepartamento(request.getDepartamento());
        return proveedor;
    }

    public ProveedorResponse toProveedorResponse(Proveedor proveedor){
        ProveedorResponse proveedorResponse = new ProveedorResponse();
        proveedorResponse.setId(proveedor.getId());
        proveedorResponse.setNombre(proveedor.getNombre());
        proveedorResponse.setNumeroDocumento(proveedor.getNumeroDocumento());
        proveedorResponse.setTipoDocumento(proveedor.getTipoDocumento());
        proveedorResponse.setTipoServicio(proveedor.getTipoServicio());
        proveedorResponse.setPais(proveedor.getPais());
        proveedorResponse.setDepartamento(proveedor.getDepartamento());
        proveedorResponse.setCiudad(proveedor.getCiudad());
        proveedorResponse.setDireccion(proveedor.getDireccion());
        proveedorResponse.setTelefono(proveedor.getTelefono());
        proveedorResponse.setStatus(proveedor.getStatus());

        return  proveedorResponse;
    }

}

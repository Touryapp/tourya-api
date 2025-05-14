package com.tourya.api.services;


import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Proveedor;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.ProveedorMapper;
import com.tourya.api.models.responses.ProveedorResponse;
import com.tourya.api.models.resquest.ProveedorRequest;
import com.tourya.api.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProveedorService {
    private final ProveedorRepository proveedorRepository;

    private final ProveedorMapper proveedorMapper;

    public Proveedor save(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public ProveedorResponse update(ProveedorRequest proveedorRequest,
                                    Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        Proveedor proveedor = proveedorRepository.findByUser(user);
        if(proveedor != null){
            proveedor.setNombre(proveedorRequest.getNombre());
            proveedor.setNumeroDocumento(proveedorRequest.getNumeroDocumento());
            proveedor.setTipoDocumento(proveedorRequest.getTipoDocumento());
            proveedor.setPais(proveedorRequest.getPais());
            proveedor.setDepartamento(proveedorRequest.getDepartamento());
            proveedor.setCiudad(proveedorRequest.getCiudad());
            proveedor.setDireccion(proveedorRequest.getDireccion());
            proveedor.setTelefono(proveedorRequest.getTelefono());
            return proveedorMapper.toProveedorResponse(proveedorRepository.save(proveedor));
        }else{
            throw new ResourceNotFoundException("Resource not found.");
        }

    }

    public ProveedorResponse consultarProveedor(Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        Proveedor proveedor = proveedorRepository.findByUser(user);
        if(proveedor != null){
            return proveedorMapper.toProveedorResponse(proveedor);
        }else{
            throw new ResourceNotFoundException("Resource not found.");
        }

    }

    public Proveedor findByUser(User user) {
        return proveedorRepository.findByUser(user);
    }
}

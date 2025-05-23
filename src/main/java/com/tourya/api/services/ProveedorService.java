package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ProveedorStatusEnum;
import com.tourya.api.constans.enums.SolicitudStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Proveedor;
import com.tourya.api.models.Role;
import com.tourya.api.models.Solicitud;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.ProveedorMapper;
import com.tourya.api.models.responses.ProveedorResponse;
import com.tourya.api.models.resquest.ProveedorRequest;
import com.tourya.api.repository.ProveedorRepository;
import com.tourya.api.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProveedorService {
    private final ProveedorRepository proveedorRepository;

    private final ProveedorMapper proveedorMapper;

    private final SolicitudRepository solicitudRepository;

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
            proveedor.setTipoServicio(proveedorRequest.getTipoServicio());
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

    public ProveedorResponse consultDataProveedor(Authentication connectedUser){
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

    public Proveedor findById(Integer proveedorId) {
        Optional<Proveedor>  optionalProveedor = proveedorRepository.findById(proveedorId);
        if(optionalProveedor.isPresent()){
            return  optionalProveedor.get();
        }else{
            return null;
        }
    }

    public PageResponse<ProveedorResponse> findAll(int page, int size, ProveedorStatusEnum status, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<Proveedor> allProveedores = proveedorRepository.findAllProveedor(status, pageable);
            List<ProveedorResponse> proveedoresResponse = allProveedores.stream()
                    .map(proveedorMapper::toProveedorResponse)
                    .toList();

            return new PageResponse<>(
                    proveedoresResponse,
                    allProveedores.getNumber(),
                    allProveedores.getSize(),
                    allProveedores.getTotalElements(),
                    allProveedores.getTotalPages(),
                    allProveedores.isFirst(),
                    allProveedores.isLast()
            );
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    public ProveedorResponse consultDataProveedorById(Integer proveedorId, Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if (Utils.isAdmin(roleList)) {
            Proveedor proveedor = this.findById(proveedorId);
            if(proveedor != null){
                return proveedorMapper.toProveedorResponse(proveedor);
            }else{
                throw new ResourceNotFoundException("Proveedor not found. Id:"+proveedorId);
            }
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    public void delecteProveedorById(Integer proveedorId, Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if (Utils.isAdmin(roleList)) {
            Proveedor proveedor = findById(proveedorId);
            if (proveedor != null) {
                Solicitud solicitud = this.getSolicitudByProveedor(proveedor);
                if (solicitud == null) {
                    proveedorRepository.delete(proveedor);
                } else {
                    throw new OperationNotPermittedException("No se puede elimninar el proveedor, se encontro una solicitud asociada.");
                }
            } else {
                throw new ResourceNotFoundException("Proveedor no encontrado. id = " + proveedorId);
            }
        } else {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    @Transactional
    public ProveedorResponse activeOrInactiveProveedorById(Integer proveedorId, ProveedorStatusEnum status, Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if (Utils.isAdmin(roleList)) {
            Proveedor proveedor = findById(proveedorId);
            if (proveedor != null) {
                proveedor.setStatus(status);
                Proveedor proveedorUpdate = proveedorRepository.save(proveedor);
                Solicitud solicitud = this.getSolicitudByProveedor(proveedor);
                if (solicitud != null) {
                    solicitud.setProveedor(proveedorUpdate);
                    solicitud.setStatus(status.equals(ProveedorStatusEnum.ACTIVO) ?
                            SolicitudStatusEnum.COMPLETADA : SolicitudStatusEnum.RECHAZADA);
                    solicitudRepository.save(solicitud);
                    return proveedorMapper.toProveedorResponse(proveedorUpdate);
                } else {
                    throw new OperationNotPermittedException("No se encontro solicitud asociada al proveedor.");
                }
            } else {
                throw new ResourceNotFoundException("Proveedor no encontrado. id = " + proveedorId);
            }
        } else {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    public Solicitud getSolicitudByProveedor(Proveedor proveedor){
        return solicitudRepository.findByProveedor(proveedor);
    }
}

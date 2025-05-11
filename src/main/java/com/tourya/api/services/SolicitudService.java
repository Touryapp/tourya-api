package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Proveedor;
import com.tourya.api.models.Role;
import com.tourya.api.models.Solicitud;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.ProveedorMapper;
import com.tourya.api.models.mapper.SolicitudMapper;
import com.tourya.api.models.responses.SolicitudResponse;
import com.tourya.api.models.resquest.SolicitudRequest;
import com.tourya.api.repository.ProveedorRepository;
import com.tourya.api.repository.RoleRepository;
import com.tourya.api.repository.SolicitudRepository;
import com.tourya.api.repository.UserRepository;
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
public class SolicitudService {
    private final SolicitudRepository solicitudRepository;
    private final ProveedorService proveedorService;
    private final ProveedorMapper proveedorMapper;
    private final SolicitudMapper solicitudMapper;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    @Transactional
    public SolicitudResponse save(SolicitudRequest request,
                                  Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());

        List<Role> roleList =  user.getRoles();

        var userRole = roleRepository.findByName("PROVEEDOR")
                // todo - better exception handling
                .orElseThrow(() -> new IllegalStateException("ROLE PROVEEDOR was not initiated"));
        roleList.add(userRole);
        user.setRoles(roleList);
        User userUpdate = userRepository.save(user);


        Proveedor proveedor = proveedorMapper.toProveedor(request);
        proveedor.setUser(userUpdate);
        proveedor.setStatus("pendiente");
        Proveedor proveedorNuevo = proveedorService.save(proveedor);

        Solicitud solicitud = new Solicitud();
        solicitud.setProveedor(proveedorNuevo);
        solicitud.setStatus("solicitado");

        Solicitud solicitudNueva = solicitudRepository.save(solicitud);

        return solicitudMapper.toSolicitudResponse(solicitudNueva);
    }

    public SolicitudResponse getSolicitudByUser(Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());

        Proveedor proveedor = proveedorService.findByUser(user);
        if(proveedor != null){
            Solicitud solicitud = solicitudRepository.findByProveedor(proveedor);
            return solicitudMapper.toSolicitudResponse(solicitud);
        }else{
            return null;
        }
    }
    public SolicitudResponse getSolicitudById(Integer solicitudId,
                                              Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
            if(solicitudOpt.isPresent()){
                Solicitud solicitud = solicitudOpt.get();
                return solicitudMapper.toSolicitudResponse(solicitud);
            }else{
                throw new ResourceNotFoundException("Resource not found.");
            }
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    @Transactional
    public SolicitudResponse aprobarSolicitudById(Integer solicitudId,
                                              Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
            if(solicitudOpt.isPresent()){
                Solicitud solicitud = solicitudOpt.get();

                Proveedor proveedor = solicitud.getProveedor();
                proveedor.setStatus("activo");
                proveedorService.save(proveedor);

                solicitud.setStatus("chequeada");
                Solicitud solicitudUpdate = solicitudRepository.save(solicitud);

                return solicitudMapper.toSolicitudResponse(solicitudUpdate);
            }else{
                throw new ResourceNotFoundException("Resource not found.");
            }
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    @Transactional
    public SolicitudResponse declinarSolicitudById(Integer solicitudId,
                                                  Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<Solicitud> solicitudOpt = solicitudRepository.findById(solicitudId);
            if(solicitudOpt.isPresent()){
                Solicitud solicitud = solicitudOpt.get();

                Proveedor proveedor = solicitud.getProveedor();
                proveedor.setStatus("inactivo");
                proveedorService.save(proveedor);

                solicitud.setStatus("chequeada");
                Solicitud solicitudUpdate = solicitudRepository.save(solicitud);

                return solicitudMapper.toSolicitudResponse(solicitudUpdate);
            }else{
                throw new ResourceNotFoundException("Resource not found.");
            }
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    public PageResponse<SolicitudResponse> getSolicitudesAll(int page, int size, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<Solicitud> allSolicitudesPendientes = solicitudRepository.findAllSolicitudesPendientes(pageable);

            List<SolicitudResponse> solicitudesResponse = allSolicitudesPendientes.stream()
                    .map(solicitudMapper::toSolicitudResponse)
                    .toList();
            return new PageResponse<>(
                    solicitudesResponse,
                    allSolicitudesPendientes.getNumber(),
                    allSolicitudesPendientes.getSize(),
                    allSolicitudesPendientes.getTotalElements(),
                    allSolicitudesPendientes.getTotalPages(),
                    allSolicitudesPendientes.isFirst(),
                    allSolicitudesPendientes.isLast()
            );
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

}

package com.tourya.api.services;

import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.ServiceType;
import com.tourya.api.models.mapper.ServiceTypeMapper;
import com.tourya.api.models.responses.ServiceTypeResponse;
import com.tourya.api.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de tipos de servicio.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceTypeMapper serviceTypeMapper;

    /**
     * Obtiene todos los tipos de servicio con paginación.
     * 
     * @param pageable parámetros de paginación
     * @return Page con los tipos de servicio
     */
    @Transactional(readOnly = true)
    public Page<ServiceTypeResponse> getAllServiceTypes(Pageable pageable) {
        return serviceTypeRepository.findAll(pageable)
                .map(serviceTypeMapper::toResponse);
    }

    /**
     * Obtiene un tipo de servicio por su ID.
     * 
     * @param id ID del tipo de servicio
     * @return ServiceTypeResponse
     */
    @Transactional(readOnly = true)
    public ServiceTypeResponse getServiceTypeById(Integer id) {
        ServiceType serviceType = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado"));
        return serviceTypeMapper.toResponse(serviceType);
    }

    /**
     * Obtiene todos los tipos de servicio activos.
     * 
     * @return Lista de tipos de servicio activos
     */
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> getActiveServiceTypes() {
        return serviceTypeRepository.findActiveServiceTypes()
                .stream()
                .map(serviceTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un tipo de servicio por nombre.
     * 
     * @param name nombre del tipo de servicio
     * @return ServiceTypeResponse
     */
    @Transactional(readOnly = true)
    public ServiceTypeResponse getServiceTypeByName(String name) {
        ServiceType serviceType = serviceTypeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con nombre: " + name));
        return serviceTypeMapper.toResponse(serviceType);
    }
}



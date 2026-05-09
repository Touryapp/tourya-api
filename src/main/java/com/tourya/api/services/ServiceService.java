package com.tourya.api.services;

import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.TouryaService;
import com.tourya.api.models.ServiceType;
import com.tourya.api.models.mapper.ServiceMapper;
import com.tourya.api.models.responses.ServiceResponse;
import com.tourya.api.repository.ServiceRepository;
import com.tourya.api.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de servicios.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceMapper serviceMapper;

    /**
     * Obtiene todos los servicios con paginación.
     * 
     * @param pageable parámetros de paginación
     * @return Page con los servicios
     */
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getAllServices(Pageable pageable) {
        return serviceRepository.findAll(pageable)
                .map(serviceMapper::toResponse);
    }

    /**
     * Obtiene un servicio por su ID.
     * 
     * @param id ID del servicio
     * @return ServiceResponse
     */
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Integer id) {
        TouryaService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        return serviceMapper.toResponse(service);
    }

    /**
     * Obtiene todos los servicios activos.
     * 
     * @return Lista de servicios activos
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getActiveServices() {
        return serviceRepository.findActiveServices()
                .stream()
                .map(serviceMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene servicios por tipo de servicio.
     * 
     * @param serviceTypeId ID del tipo de servicio
     * @return Lista de servicios del tipo especificado
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByType(Integer serviceTypeId) {
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado"));
        
        return serviceRepository.findByServiceType(serviceType)
                .stream()
                .map(serviceMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene servicios activos por tipo de servicio.
     * 
     * @param serviceTypeId ID del tipo de servicio
     * @return Lista de servicios activos del tipo especificado
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getActiveServicesByType(Integer serviceTypeId) {
        return serviceRepository.findActiveServicesByType(serviceTypeId)
                .stream()
                .map(serviceMapper::toResponse)
                .collect(Collectors.toList());
    }
}
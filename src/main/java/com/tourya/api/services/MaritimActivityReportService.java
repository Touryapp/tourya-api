package com.tourya.api.services;

import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.MaritimActivityReport;
import com.tourya.api.models.request.MaritimActivityReportRequest;
import com.tourya.api.models.responses.MaritimActivityReportResponse;
import com.tourya.api.repository.MaritimActivityReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de reportes de actividades marítimas DIMAR.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MaritimActivityReportService {

    private final MaritimActivityReportRepository maritimActivityReportRepository;

    /**
     * Crea un nuevo reporte DIMAR.
     * 
     * @param request Datos del reporte
     * @param authentication Usuario autenticado
     * @return MaritimActivityReportResponse con el reporte creado
     */
    @Transactional
    public MaritimActivityReportResponse create(MaritimActivityReportRequest request, Authentication authentication) {
        log.info("Creating maritime activity report: country={}, city={}, activity={}, flag={}, date={}", 
                request.getCountry(), request.getCity(), request.getActivity(), request.getFlag(), request.getReportDate());
        
        MaritimActivityReport report = MaritimActivityReport.builder()
                .country(request.getCountry())
                .city(request.getCity())
                .activity(request.getActivity())
                .flag(request.getFlag())
                .reportDate(request.getReportDate())
                .build();
        
        report = maritimActivityReportRepository.save(report);
        
        return toResponse(report);
    }

    /**
     * Obtiene todos los reportes con paginación.
     * 
     * @param page Número de página (0-based)
     * @param size Tamaño de página
     * @return PageResponse con los reportes
     */
    @Transactional(readOnly = true)
    public PageResponse<MaritimActivityReportResponse> findAll(Integer page, Integer size) {
        log.info("Getting all maritime activity reports - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());
        Page<MaritimActivityReport> reportsPage = maritimActivityReportRepository.findAll(pageable);
        
        List<MaritimActivityReportResponse> responses = reportsPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return PageResponse.<MaritimActivityReportResponse>builder()
                .content(responses)
                .number(reportsPage.getNumber())
                .size(reportsPage.getSize())
                .totalElements(reportsPage.getTotalElements())
                .totalPages(reportsPage.getTotalPages())
                .first(reportsPage.isFirst())
                .last(reportsPage.isLast())
                .build();
    }

    /**
     * Obtiene un reporte por su ID.
     * 
     * @param id ID del reporte
     * @return MaritimActivityReportResponse
     */
    @Transactional(readOnly = true)
    public MaritimActivityReportResponse findById(Long id) {
        log.info("Getting maritime activity report by id: {}", id);
        
        MaritimActivityReport report = maritimActivityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maritime activity report not found with id: " + id));
        
        return toResponse(report);
    }

    /**
     * Actualiza un reporte existente.
     * 
     * @param id ID del reporte a actualizar
     * @param request Datos actualizados del reporte
     * @return MaritimActivityReportResponse con el reporte actualizado
     */
    @Transactional
    public MaritimActivityReportResponse update(Long id, MaritimActivityReportRequest request) {
        log.info("Updating maritime activity report id: {}", id);
        
        MaritimActivityReport report = maritimActivityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maritime activity report not found with id: " + id));
        
        report.setCountry(request.getCountry());
        report.setCity(request.getCity());
        report.setActivity(request.getActivity());
        report.setFlag(request.getFlag());
        report.setReportDate(request.getReportDate());
        
        report = maritimActivityReportRepository.save(report);
        
        return toResponse(report);
    }

    /**
     * Elimina un reporte por su ID.
     * 
     * @param id ID del reporte a eliminar
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting maritime activity report id: {}", id);
        
        if (!maritimActivityReportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Maritime activity report not found with id: " + id);
        }
        
        maritimActivityReportRepository.deleteById(id);
    }

    /**
     * Busca reportes por fecha.
     * 
     * @param reportDate Fecha del reporte
     * @return Lista de reportes de esa fecha
     */
    @Transactional(readOnly = true)
    public List<MaritimActivityReportResponse> findByReportDate(LocalDate reportDate) {
        log.info("Getting maritime activity reports by date: {}", reportDate);
        
        return maritimActivityReportRepository.findByReportDate(reportDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca reportes por país y ciudad.
     * 
     * @param country País
     * @param city Ciudad
     * @return Lista de reportes
     */
    @Transactional(readOnly = true)
    public List<MaritimActivityReportResponse> findByCountryAndCity(String country, String city) {
        log.info("Getting maritime activity reports by country: {} and city: {}", country, city);
        
        return maritimActivityReportRepository.findByCountryAndCity(country, city).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad a Response DTO.
     */
    private MaritimActivityReportResponse toResponse(MaritimActivityReport report) {
        return MaritimActivityReportResponse.builder()
                .id(report.getId())
                .country(report.getCountry())
                .city(report.getCity())
                .activity(report.getActivity())
                .flag(report.getFlag())
                .reportDate(report.getReportDate())
                .createdDate(report.getCreatedDate())
                .lastModifiedDate(report.getLastModifiedDate())
                .build();
    }
}



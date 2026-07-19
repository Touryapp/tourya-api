package com.tourya.api.services;

import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.City;
import com.tourya.api.models.Country;
import com.tourya.api.models.MaritimActivityReport;
import com.tourya.api.models.State;
import com.tourya.api.models.request.MaritimActivityReportRequest;
import com.tourya.api.models.responses.MaritimActivityReportResponse;
import com.tourya.api.constans.enums.MaritimeFlagEnum;
import com.tourya.api.repository.CityRepository;
import com.tourya.api.repository.CountryRepository;
import com.tourya.api.repository.MaritimActivityReportRepository;
import com.tourya.api.repository.StateRepository;
import com.tourya.api.services.maritime.events.MaritimeAlertCreatedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MaritimActivityReportService {

    private final MaritimActivityReportRepository maritimActivityReportRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;
    private final ApplicationEventPublisher eventPublisher; // BE-23

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public MaritimActivityReportResponse create(MaritimActivityReportRequest request, Authentication authentication) {
        validateReportDates(request.getReportStartDate(), request.getReportEndDate());
        LocationRefs location = resolveAndValidateLocation(request);
        validateCategoryAndSubcategory(request.getBusinessCategoryId(), request.getSubcategoryCode());

        MaritimActivityReport report = MaritimActivityReport.builder()
                .country(location.country())
                .state(location.state())
                .city(location.city())
                .businessCategoryId(request.getBusinessCategoryId())
                .subcategoryCode(request.getSubcategoryCode())
                .flag(request.getFlag())
                .reportStartDate(request.getReportStartDate())
                .reportEndDate(request.getReportEndDate())
                .build();

        MaritimActivityReport saved = maritimActivityReportRepository.save(report);

        // BE-23: al crear un reporte RED, publicar evento para que un listener
        // AFTER_COMMIT + @Async cancele las reservas afectadas + genere creditos.
        // Solo se dispara para RED — GREEN/YELLOW son informativos.
        if (saved.getFlag() == MaritimeFlagEnum.RED) {
            eventPublisher.publishEvent(new MaritimeAlertCreatedEvent(
                    saved.getId(),
                    saved.getSubcategoryCode(),
                    saved.getCountry() != null ? saved.getCountry().getId() : null,
                    saved.getState() != null ? saved.getState().getId() : null,
                    saved.getCity() != null ? saved.getCity().getId() : null,
                    saved.getReportStartDate(),
                    saved.getReportEndDate(),
                    saved.getFlag()));
            log.info("BE-23 MaritimeAlertCreatedEvent published for RED report {}", saved.getId());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<MaritimActivityReportResponse> findAll(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportStartDate").descending());
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

    @Transactional(readOnly = true)
    public MaritimActivityReportResponse findById(Long id) {
        MaritimActivityReport report = maritimActivityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maritime activity report not found with id: " + id));
        return toResponse(report);
    }

    @Transactional
    public MaritimActivityReportResponse update(Long id, MaritimActivityReportRequest request) {
        MaritimActivityReport report = maritimActivityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maritime activity report not found with id: " + id));

        validateReportDates(request.getReportStartDate(), request.getReportEndDate());
        LocationRefs location = resolveAndValidateLocation(request);
        validateCategoryAndSubcategory(request.getBusinessCategoryId(), request.getSubcategoryCode());

        report.setCountry(location.country());
        report.setState(location.state());
        report.setCity(location.city());
        report.setBusinessCategoryId(request.getBusinessCategoryId());
        report.setSubcategoryCode(request.getSubcategoryCode());
        report.setFlag(request.getFlag());
        report.setReportStartDate(request.getReportStartDate());
        report.setReportEndDate(request.getReportEndDate());

        return toResponse(maritimActivityReportRepository.save(report));
    }

    @Transactional
    public void delete(Long id) {
        if (!maritimActivityReportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Maritime activity report not found with id: " + id);
        }
        maritimActivityReportRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<MaritimActivityReportResponse> findActiveOnDate(LocalDate reportDate) {
        return maritimActivityReportRepository.findActiveOnDate(reportDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaritimActivityReportResponse> findByLocation(
            Integer countryId, Integer stateId, Integer cityId) {
        return maritimActivityReportRepository.findByLocationIds(countryId, stateId, cityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validateReportDates(LocalDate reportStartDate, LocalDate reportEndDate) {
        LocalDate today = LocalDate.now();
        if (reportStartDate.isBefore(today)) {
            throw new OperationNotPermittedException(
                    "La fecha de inicio del reporte no puede ser anterior a hoy.");
        }
        if (reportEndDate.isBefore(today)) {
            throw new OperationNotPermittedException(
                    "La fecha de fin del reporte no puede ser anterior a hoy.");
        }
        if (reportEndDate.isBefore(reportStartDate)) {
            throw new OperationNotPermittedException(
                    "La fecha de fin del reporte no puede ser anterior a la fecha de inicio.");
        }
    }

    private LocationRefs resolveAndValidateLocation(MaritimActivityReportRequest request) {
        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + request.getCountryId()));
        State state = stateRepository.findById(request.getStateId())
                .orElseThrow(() -> new ResourceNotFoundException("State not found with id: " + request.getStateId()));
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + request.getCityId()));

        if (!state.getCountry().getId().equals(country.getId())) {
            throw new OperationNotPermittedException("El departamento no pertenece al país indicado.");
        }
        if (!city.getState().getId().equals(state.getId())) {
            throw new OperationNotPermittedException("La ciudad no pertenece al departamento indicado.");
        }

        return new LocationRefs(country, state, city);
    }

    private void validateCategoryAndSubcategory(Integer businessCategoryId, String subcategoryCode) {
        Number categoryCount = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM public.tour_business_category WHERE id = :id")
                .setParameter("id", businessCategoryId)
                .getSingleResult();
        if (categoryCount.longValue() == 0) {
            throw new ResourceNotFoundException("Business category not found with id: " + businessCategoryId);
        }

        Number mappingCount = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM public.tour_business_subcategory_mapping
                        WHERE subcategory_code = :code AND business_category_id = :categoryId
                        """)
                .setParameter("code", subcategoryCode)
                .setParameter("categoryId", businessCategoryId)
                .getSingleResult();
        if (mappingCount.longValue() == 0) {
            throw new OperationNotPermittedException(
                    "La subcategoría no pertenece a la categoría indicada.");
        }
    }

    private MaritimActivityReportResponse toResponse(MaritimActivityReport report) {
        return MaritimActivityReportResponse.builder()
                .id(report.getId())
                .countryId(report.getCountry() != null ? report.getCountry().getId() : null)
                .countryName(report.getCountry() != null ? report.getCountry().getName() : null)
                .stateId(report.getState() != null ? report.getState().getId() : null)
                .stateName(report.getState() != null ? report.getState().getName() : null)
                .cityId(report.getCity() != null ? report.getCity().getId() : null)
                .cityName(report.getCity() != null ? report.getCity().getName() : null)
                .businessCategoryId(report.getBusinessCategoryId())
                .subcategoryCode(report.getSubcategoryCode())
                .flag(report.getFlag())
                .reportStartDate(report.getReportStartDate())
                .reportEndDate(report.getReportEndDate())
                .createdDate(report.getCreatedDate())
                .lastModifiedDate(report.getLastModifiedDate())
                .build();
    }

    private record LocationRefs(Country country, State state, City city) {}
}

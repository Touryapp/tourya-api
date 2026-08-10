package com.tourya.api.services;

import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api._utils.Utils;
import com.tourya.api.models.City;
import com.tourya.api.models.Country;
import com.tourya.api.models.MaritimActivityReport;
import com.tourya.api.models.State;
import com.tourya.api.models.User;
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
        // FE-15b + H2: hasta ahora el endpoint POST /maritime-activity-reports
        // solo pedia authenticated() en SecurityConfig — cualquier usuario podia
        // crear reportes DIMAR. Restringido a ADMIN + BACKOFFICE_OPERATION (Luis P2 opción C).

        // TC-018 (#227 3ra iter): log de entrada — Luis reporto que el hook no dispara aunque
        // el reporte se cree con flag=RED. Instrumentacion para saber si `create()` corre
        // y con que flag llega el request antes de cualquier validacion.
        log.info("BE-23 create() called with flag={} subcategory={} dates={}..{}",
                request.getFlag(), request.getSubcategoryCode(),
                request.getReportStartDate(), request.getReportEndDate());

        requireBackoffice(authentication);
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

        // TC-018 (#227 3ra iter): log del save + evaluacion del if.
        log.info("BE-23 saved report id={}, flag={}, isRed={}",
                saved.getId(), saved.getFlag(), saved.getFlag() == MaritimeFlagEnum.RED);

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
        } else {
            log.info("BE-23 skipping event publish for report {} — flag is {}", saved.getId(), saved.getFlag());
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
    public MaritimActivityReportResponse update(Long id, MaritimActivityReportRequest request, Authentication authentication) {
        // FE-15b + H2: mismo guard que create.
        requireBackoffice(authentication);
        MaritimActivityReport report = maritimActivityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maritime activity report not found with id: " + id));

        validateReportDates(request.getReportStartDate(), request.getReportEndDate());
        LocationRefs location = resolveAndValidateLocation(request);
        validateCategoryAndSubcategory(request.getBusinessCategoryId(), request.getSubcategoryCode());

        // TC-018 (#227) Bug A hipotesis 1: capturar flag previo para saber si esta edicion
        // TRANSICIONA el reporte a RED. En ese caso debemos publicar MaritimeAlertCreatedEvent
        // igual que en create(), para que el listener AFTER_COMMIT cancele reservas.
        MaritimeFlagEnum previousFlag = report.getFlag();

        report.setCountry(location.country());
        report.setState(location.state());
        report.setCity(location.city());
        report.setBusinessCategoryId(request.getBusinessCategoryId());
        report.setSubcategoryCode(request.getSubcategoryCode());
        report.setFlag(request.getFlag());
        report.setReportStartDate(request.getReportStartDate());
        report.setReportEndDate(request.getReportEndDate());

        MaritimActivityReport saved = maritimActivityReportRepository.save(report);

        // TC-018 (#227): dispara el hook si el reporte ES RED tras la edicion. Cubre dos casos:
        //   (a) transicion GREEN/YELLOW -> RED,
        //   (b) reporte ya RED pero editado (p.ej. cambio de subcategoria, ubicacion o rango
        //       de fechas) — necesitamos re-evaluar reservas afectadas con los nuevos criterios.
        // El listener es idempotente por reserva (skip si ya CANCELED).
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
            log.info("BE-23 MaritimeAlertCreatedEvent published for RED report {} (update, previousFlag={})",
                    saved.getId(), previousFlag);
        }

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        // FE-15b + H2: mismo guard que create/update.
        requireBackoffice(authentication);
        if (!maritimActivityReportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Maritime activity report not found with id: " + id);
        }
        maritimActivityReportRepository.deleteById(id);
    }

    /**
     * FE-15b + H2: cierra un agujero de seguridad preexistente. Hasta este cambio
     * el endpoint POST/PUT/DELETE de reportes solo requeria autenticacion generica
     * (SecurityConfig anyRequest().authenticated()). Ahora exige ADMIN o
     * BACKOFFICE_OPERATION (subset P2 opción C de Luis: "gestionar reportes DIMAR").
     */
    private void requireBackoffice(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new InsufficientPrivilegesException("Autenticacion requerida.");
        }
        if (!Utils.isTouryaBackoffice(user.getRoles())) {
            throw new InsufficientPrivilegesException(
                    "Solo ADMIN o BACKOFFICE_OPERATION pueden gestionar reportes DIMAR.");
        }
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

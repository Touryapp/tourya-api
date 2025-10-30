package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.request.*;
import com.tourya.api.models.responses.*;
import com.tourya.api.models.specification.TourScheduleSpecification;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.TourScheduleConfigRepository;
import com.tourya.api.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourScheduleConfigGeneralService {
    private final TourScheduleRepository tourScheduleRepository;
    private final TourScheduleConfigRepository tourScheduleConfigRepository;
    private final TourRepository tourRepository;
    private final ProviderService providerService;
    private final TourAddressRepository tourAddressRepository; // <-- INYECTAR REPOSITORIO
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    private Tour getTour(Integer tourId, Integer providerId){
        Tour tour =  tourRepository.findTourByIdAndProviderId(tourId, providerId);
        if(tour != null){
            return tour;
        }else{
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }


    @Transactional
    public TourScheduleConfigResponse createTourScheduleConfig(
            TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Provider provider = providerService.findByUserAndStatusActive(user);

        // 1. Construir el grafo de entidades a partir del DTO
        TourScheduleConfig config = buildConfigFromRequest(request ,provider);
        Set<TourScheduleConfigSlot> slots = buildSlotsAndPricesFromRequest(request.getSlots(), config);
        config.setSlots(slots);

        // 2. Guardar la configuración completa
        TourScheduleConfig savedConfig = tourScheduleConfigRepository.save(config);


        // 4. Mapear a la respuesta directamente sin volver a consultar la BD
        return mapToTourScheduleConfigResponse(savedConfig);
    }

    private TourScheduleConfig buildConfigFromRequest(TourScheduleConfigCreationRequest request, Provider provider) {
        TourScheduleConfig config = new TourScheduleConfig();
        config.setLabel(request.getLabel());
        config.setProvider(provider);
        config.setProviderId(provider.getId());
        config.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));
        config.setIsUnlimitedCapacity(request.getIsUnlimitedCapacity() != null && request.getIsUnlimitedCapacity());
        config.setIsTemplate(request.getIsTemplate());
        return config;
    }

    private Set<TourScheduleConfigSlot> buildSlotsAndPricesFromRequest(Set<TourScheduleConfigSlotDto> slotDtos, TourScheduleConfig config) {
        if (slotDtos == null) {
            return new HashSet<>();
        }
        Set<TourScheduleConfigSlot> slots = new HashSet<>();
        for (TourScheduleConfigSlotDto slotDto : slotDtos) {
            // Validar los precios ANTES de construir las entidades
            validateSlotPrices(new HashSet<>(slotDto.getPrices()));

            TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
            slot.setConfig(config);
            slot.setStartTime(slotDto.getStartTime());
            slot.setEndTime(slotDto.getEndTime());
            slot.setMinCapacity(slotDto.getMinCapacity());
            slot.setMaxCapacity(slotDto.getMaxCapacity());

            if (slotDto.getPrices() != null) {
                Set<TourScheduleConfigPrice> prices = new HashSet<>();
                for (TourScheduleConfigPriceDto priceDto : slotDto.getPrices()) {
                    TourScheduleConfigPrice price = new TourScheduleConfigPrice();
                    price.setSlot(slot);
                    price.setAgeType(priceDto.getAgeType());
                    price.setMinAge(priceDto.getMinAge());
                    price.setMaxAge(priceDto.getMaxAge());
                    price.setPrice(priceDto.getPrice());
                    prices.add(price);
                }
                slot.setPrices(prices);
            }
            slots.add(slot);
        }
        return slots;
    }

    private Set<DayOfWeek> getValidDaysOfWeek(List<String> daysOfWeek) {
        return daysOfWeek.stream()
                .map(String::toUpperCase)
                .map(dayStr -> {
                    try {
                        return DayOfWeek.valueOf(dayStr);
                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Invalid day of the week: " + dayStr + ". It must be one of the java.time.DayOfWeek values (e.g., MONDAY).");
                    }
                })
                .collect(Collectors.toSet());
    }

    @Transactional
    public TourScheduleConfigResponse updateTourScheduleConfig(
            Integer configId, TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Provider provider = providerService.findByUserAndStatusActive(user);

        // 1. Obtener la configuración existente
        TourScheduleConfig existingConfig = tourScheduleConfigRepository.findByIdWithSlots(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour configuration with ID " + configId + " not found."));

        // 3. Actualizar las propiedades y colecciones de la entidad
        updateConfigProperties(existingConfig, request);
        manageSlotsUpdate(existingConfig, request.getSlots());

        // 4. Guardar la entidad actualizada
        TourScheduleConfig savedConfig = tourScheduleConfigRepository.save(existingConfig);

        // 5. Regenerar los horarios
        Set<DayOfWeek> newValidDaysOfWeek = getValidDaysOfWeek(request.getDaysOfWeek());

        // 6. Mapear y devolver la respuesta
        return mapToTourScheduleConfigResponse(savedConfig);
    }

    private void validateUpdateRequestAgainstExistingReservations(TourScheduleConfig existingConfig, TourScheduleConfigCreationRequest request) {
        List<TourSchedule> existingSchedules = tourScheduleRepository.findByConfigId(existingConfig.getId());

        Set<String> reservedScheduleKeys = existingSchedules.stream()
                .filter(s -> s.getReservedCapacity() > 0)
                .map(this::generateScheduleKey)
                .collect(Collectors.toSet());

        // Si no hay reservas, no hay nada que validar. Permitir cualquier cambio.
        if (reservedScheduleKeys.isEmpty()) {
            return;
        }
    }

    private String generateScheduleKey(TourSchedule schedule) {
        return schedule.getScheduleDate() +"";
    }

    private void updateConfigProperties(TourScheduleConfig existingConfig, TourScheduleConfigCreationRequest request) {
        existingConfig.setLabel(request.getLabel());
        existingConfig.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));
        existingConfig.setIsUnlimitedCapacity(request.getIsUnlimitedCapacity() != null && request.getIsUnlimitedCapacity());
        existingConfig.setIsTemplate(request.getIsTemplate()); // <-- Mapear isTemplate
    }

    private void manageSlotsUpdate(TourScheduleConfig existingConfig, Set<TourScheduleConfigSlotDto> requestedSlots) {
        Map<Integer, TourScheduleConfigSlot> existingSlotsMap = existingConfig.getSlots().stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(TourScheduleConfigSlot::getId, Function.identity()));

        Set<TourScheduleConfigSlot> newOrUpdatedSlots = new HashSet<>();

        if (requestedSlots != null) {
            for (TourScheduleConfigSlotDto slotDto : requestedSlots) {
                TourScheduleConfigSlot currentSlot;
                if (slotDto.getId() != null && existingSlotsMap.containsKey(slotDto.getId())) {
                    // Slot existente para actualizar
                    currentSlot = existingSlotsMap.get(slotDto.getId());
                } else {
                    // Nuevo slot para crear
                    currentSlot = new TourScheduleConfigSlot();
                    currentSlot.setConfig(existingConfig);
                }
                currentSlot.setStartTime(slotDto.getStartTime());
                currentSlot.setEndTime(slotDto.getEndTime());
                currentSlot.setMinCapacity(slotDto.getMinCapacity());
                currentSlot.setMaxCapacity(slotDto.getMaxCapacity());

                updateSlotPrices(currentSlot, new HashSet<>(slotDto.getPrices()));
                newOrUpdatedSlots.add(currentSlot);
            }
        }

        // Sincronizar la colección: eliminar los que ya no están y añadir los nuevos/actualizados
        existingConfig.getSlots().clear();
        existingConfig.getSlots().addAll(newOrUpdatedSlots);
    }

    private void updateSlotPrices(TourScheduleConfigSlot slot, Set<TourScheduleConfigPriceDto> incomingPriceDtos) {
        // Validar los precios ANTES de cualquier modificación
        validateSlotPrices(incomingPriceDtos);

        Map<Integer, TourScheduleConfigPriceDto> incomingPriceDtosById = incomingPriceDtos != null ?
                incomingPriceDtos.stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(TourScheduleConfigPriceDto::getId, Function.identity()))
                : new HashMap<>();

        List<TourScheduleConfigPrice> pricesToDelete = slot.getPrices().stream()
                .filter(existingPrice -> !incomingPriceDtosById.containsKey(existingPrice.getId()))
                .collect(Collectors.toList());

        slot.getPrices().removeAll(pricesToDelete);

        if (incomingPriceDtos != null) {
            for (TourScheduleConfigPriceDto priceDto : incomingPriceDtos) {
                TourScheduleConfigPrice currentPrice;

                if (priceDto.getId() != null) {
                    currentPrice = slot.getPrices().stream()
                            .filter(p -> p.getId().equals(priceDto.getId()))
                            .findFirst()
                            .orElse(null);
                } else {
                    currentPrice = null;
                }

                if (currentPrice == null) {
                    currentPrice = new TourScheduleConfigPrice();
                    currentPrice.setSlot(slot);
                    slot.getPrices().add(currentPrice);
                }
                currentPrice.setAgeType(priceDto.getAgeType());
                currentPrice.setMinAge(priceDto.getMinAge());
                currentPrice.setMaxAge(priceDto.getMaxAge());
                currentPrice.setPrice(priceDto.getPrice());
            }
        }
    }

    private void validateSlotPrices(Set<TourScheduleConfigPriceDto> prices) {
        if (prices == null || prices.isEmpty()) {
            return; // No hay nada que validar
        }

        // 1. Validación de ageType duplicado
        Set<AgePriceType> existingAgeTypes = new HashSet<>();
        for (TourScheduleConfigPriceDto priceDto : prices) {
            if (!existingAgeTypes.add(priceDto.getAgeType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate ageType found for the same slot: " + priceDto.getAgeType());
            }
        }

        // 2. Validación de solapamiento de rangos de edad
        List<TourScheduleConfigPriceDto> priceList = new ArrayList<>(prices);
        for (int i = 0; i < priceList.size(); i++) {
            for (int j = i + 1; j < priceList.size(); j++) {
                TourScheduleConfigPriceDto priceA = priceList.get(i);
                TourScheduleConfigPriceDto priceB = priceList.get(j);

                // Fórmula para detectar solapamiento: (InicioA <= FinB) y (FinA >= InicioB)
                if (priceA.getMinAge() <= priceB.getMaxAge() && priceA.getMaxAge() >= priceB.getMinAge()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Age ranges overlap between " + priceA.getAgeType() + " (" + priceA.getMinAge() + "-" + priceA.getMaxAge() +
                                    ") and " + priceB.getAgeType() + " (" + priceB.getMinAge() + "-" + priceB.getMaxAge() + ").");
                }
            }
        }
    }


    @Transactional(readOnly = true)
    public TourScheduleConfigResponse getTourScheduleConfigDetails(Integer configId) {
        TourScheduleConfig config = tourScheduleConfigRepository.findByIdWithSlots(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour configuration with ID " + configId + " not found."));

        List<TourSchedule> schedules = tourScheduleRepository.findByConfigId(configId);
        return mapToTourScheduleConfigResponse(config);
    }

    // 1. Consulta de TourScheduleConfig con todos sus componentes
    @Transactional(readOnly = true)
    public TourScheduleConfigResponse getConfigWithSlotsAndPrices(Integer configId) {
        TourScheduleConfig config = tourScheduleConfigRepository.findByIdWithSlots(configId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Tour configuration with ID " + configId + " not found."));
        return mapToTourScheduleConfigResponse(config);
    }

    // 2. Consulta de todos los templates de un proveedor (Opción JPA)
    @Transactional(readOnly = true)
    public List<TourScheduleConfigResponse> getTemplatesByProvider(Integer providerId) {
        List<TourScheduleConfig> templates = tourScheduleConfigRepository.findByProviderIdAndIsTemplateTrue(providerId);
        return templates.stream()
            .map(this::mapToTourScheduleConfigResponse)
            .collect(Collectors.toList());
    }

    // 2. Consulta de todos los templates de un proveedor (Opción procedimiento almacenado)
    // Si decides usar un procedimiento almacenado, crea el método en el repositorio y llama aquí:
    // public List<TourScheduleConfigResponse> getTemplatesByProviderSP(Integer providerId) {
    //     List<TourScheduleConfig> templates = tourScheduleConfigRepository.findTemplatesByProviderSP(providerId);
    //     return templates.stream()
    //         .map(this::mapToTourScheduleConfigResponse)
    //         .collect(Collectors.toList());
    // }


  /*  // Consulta de todos los templates de un proveedor usando procedimiento almacenado
    @Transactional(readOnly = true)
    public List<TourScheduleConfigResponse> getTemplatesByProviderSP(Integer providerId) {
        List<Object[]> rows = tourScheduleConfigRepository.findTemplatesByProviderSP(providerId);
        Map<Integer, TourScheduleConfigResponse> configMap = new HashMap<>();

        for (Object[] row : rows) {
            Integer configId = (Integer) row[0];
            TourScheduleConfigResponse configDto = configMap.computeIfAbsent(configId, id -> {
                TourScheduleConfigResponse dto = new TourScheduleConfigResponse();
                dto.setId(configId);
                dto.setTourId((Integer) row[1]);
                dto.setLabel((String) row[2]);
                dto.setStartDate((LocalDate) row[3]);
                dto.setEndDate((LocalDate) row[4]);
                dto.setDaysOfWeek(row[5] != null ? Arrays.asList((String[]) row[5]) : null);
                dto.setIsUnlimitedCapacity((Boolean) row[6]);
                dto.setCreatedDate((java.sql.Timestamp) row[8]);
                dto.setLastModifiedDate((java.sql.Timestamp) row[9]);
                dto.setProviderId((Integer) row[10]);
                dto.setIsTemplate((Boolean) row[11]);
                dto.setSlots(new HashSet<>());
                return dto;
            });

            // Slot
            Integer slotId = (Integer) row[12];
            if (slotId != null) {
                TourScheduleSlotResponse slotDto = configDto.getSlots().stream()
                    .filter(s -> s.getId().equals(slotId))
                    .findFirst()
                    .orElseGet(() -> {
                        TourScheduleSlotResponse s = new TourScheduleSlotResponse();
                        s.setId(slotId);
                        s.setStartTime((java.sql.Time) row[13]);
                        s.setEndTime((java.sql.Time) row[14]);
                        s.setMinCapacity((Integer) row[15]);
                        s.setMaxCapacity((Integer) row[16]);
                        s.setPrices(new HashSet<>());
                        configDto.getSlots().add(s);
                        return s;
                    });

                // Price
                Integer priceId = (Integer) row[17];
                if (priceId != null) {
                    TourSchedulePriceResponse priceDto = new TourSchedulePriceResponse();
                    priceDto.setId(priceId);
                    priceDto.setAgeType(row[18] != null ? AgePriceType.valueOf((String) row[18]) : null);
                    priceDto.setMinAge((Integer) row[19]);
                    priceDto.setMaxAge((Integer) row[20]);
                    priceDto.setPrice(row[21] != null ? new java.math.BigDecimal(row[21].toString()) : null);
                    slotDto.getPrices().add(priceDto);
                }
            }
        }
        return new ArrayList<>(configMap.values());
    }*/


    private TourScheduleConfigResponse mapToTourScheduleConfigResponse(TourScheduleConfig config) {
        TourScheduleConfigResponse responseDto = new TourScheduleConfigResponse();
        responseDto.setId(config.getId());
        responseDto.setProviderId(config.getProviderId());
        responseDto.setLabel(config.getLabel());
        responseDto.setDaysOfWeek(config.getDaysOfWeek());
        responseDto.setIsUnlimitedCapacity(config.getIsUnlimitedCapacity());

        Set<TourScheduleSlotResponse> slotDtos = config.getSlots().stream()
                .map(slot -> {
                    TourScheduleSlotResponse slotDto = new TourScheduleSlotResponse();
                    slotDto.setId(slot.getId());
                    slotDto.setStartTime(slot.getStartTime());
                    slotDto.setEndTime(slot.getEndTime());
                    slotDto.setMinCapacity(slot.getMinCapacity());
                    slotDto.setMaxCapacity(slot.getMaxCapacity());

                    Set<TourSchedulePriceResponse> priceDtos = slot.getPrices().stream()
                            .map(price -> {
                                TourSchedulePriceResponse priceDto = new TourSchedulePriceResponse();
                                priceDto.setId(price.getId());
                                priceDto.setAgeType(price.getAgeType());
                                priceDto.setMinAge(price.getMinAge());
                                priceDto.setMaxAge(price.getMaxAge());
                                priceDto.setPrice(price.getPrice());
                                return priceDto;
                            })
                            .collect(Collectors.toSet());
                    slotDto.setPrices(priceDtos);
                    return slotDto;
                })
                .collect(Collectors.toSet());
        responseDto.setSlots(slotDtos);
        return responseDto;
    }

    public List<TourScheduleResponse> findAllByTourId(Integer tourId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        List<Role> roleList = user.getRoles();

        if (Utils.isProvider(roleList)) {
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());

            // Consultar los TourSchedule por tourId
            List<TourSchedule> tourSchedules = tourScheduleRepository.findByTourId(tour.getId());

            // Por cada TourSchedule, incluir todos sus datos y los del config asociado
            return tourSchedules.stream()
                    .map(schedule -> {
                        TourScheduleResponse dto = new TourScheduleResponse();
                        dto.setId(schedule.getId());
                        dto.setTourId(schedule.getTourId());
                        dto.setScheduleDate(schedule.getScheduleDate());
                        dto.setMaxCapacity(schedule.getMaxCapacity());
                        dto.setReservedCapacity(schedule.getReservedCapacity());
                        dto.setIsUnlimitedCapacity(schedule.getIsUnlimitedCapacity());
                        dto.setStatus(schedule.getStatus());
                        dto.setConfigId(schedule.getConfigId());

                        // Agregar datos completos del config
                        if (schedule.getConfigId() != null) {
                            Optional<TourScheduleConfig> configOpt = tourScheduleConfigRepository.findByIdWithSlots(schedule.getConfigId());
                            configOpt.ifPresent(config -> {
                                TourScheduleConfigResponse configResponse = convertToTourScheduleConfigResponse(config);
                                dto.setConfig(configResponse);
                            });
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());

        } else {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }


    private TourScheduleConfigResponse convertToTourScheduleConfigResponse(TourScheduleConfig config) {
        TourScheduleConfigResponse responseDto = new TourScheduleConfigResponse();
        responseDto.setId(config.getId());
        responseDto.setProviderId(config.getProviderId());
        responseDto.setLabel(config.getLabel());
        responseDto.setDaysOfWeek(config.getDaysOfWeek());
        responseDto.setIsUnlimitedCapacity(config.getIsUnlimitedCapacity());

        if (config.getSlots() != null) {
            Set<TourScheduleSlotResponse> slotDtos = config.getSlots().stream()
                    .map(slot -> {
                        TourScheduleSlotResponse slotDto = new TourScheduleSlotResponse();
                        slotDto.setId(slot.getId());
                        slotDto.setStartTime(slot.getStartTime());
                        slotDto.setEndTime(slot.getEndTime());
                        slotDto.setMinCapacity(slot.getMinCapacity());
                        slotDto.setMaxCapacity(slot.getMaxCapacity());

                        if (slot.getPrices() != null) {
                            Set<TourSchedulePriceResponse> priceDtos = slot.getPrices().stream()
                                    .map(price -> {
                                        TourSchedulePriceResponse priceDto = new TourSchedulePriceResponse();
                                        priceDto.setId(price.getId());
                                        priceDto.setAgeType(price.getAgeType());
                                        priceDto.setMinAge(price.getMinAge());
                                        priceDto.setMaxAge(price.getMaxAge());
                                        priceDto.setPrice(price.getPrice());
                                        return priceDto;
                                    })
                                    .collect(Collectors.toSet());
                            slotDto.setPrices(priceDtos);
                        }
                        return slotDto;
                    })
                    .collect(Collectors.toSet());
            responseDto.setSlots(slotDtos);
        }

        return responseDto;
    }

    @Transactional(readOnly = true)
    public PageResponse<TourScheduleSearchResponseDto> searchToursForReservation(TourSearchRequestDto request) {
        Specification<TourSchedule> spec = TourScheduleSpecification.withSearchCriteria(request);
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDir()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Page<TourSchedule> tourSchedulesPage = tourScheduleRepository.findAll(spec, pageable);

        List<TourSchedule> schedulesOnPage = tourSchedulesPage.getContent();
        if (schedulesOnPage.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), 0, 0, 0, 0, true, true);
        }

        // Paso 1: Cargar los detalles de Tour y Config que ya vienen de la spec.
        // No necesitamos una consulta extra para esto.

        // Paso 2: Cargar eficientemente las direcciones para todos los tours de la página.
        List<Integer> tourIds = schedulesOnPage.stream()
                .map(TourSchedule::getTourId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, List<TourAddress>> addressesByTourId = tourAddressRepository.findByTourIdIn(tourIds)
                .stream()
                .collect(Collectors.groupingBy(address -> address.getTour().getId()));

        // Paso 3: Mapear a DTOs
        List<TourScheduleSearchResponseDto> responseDtos = schedulesOnPage.stream()
                .map(schedule -> {
                    TourScheduleSearchResponseDto dto = new TourScheduleSearchResponseDto();
                    dto.setScheduleId(schedule.getId());
                    dto.setScheduleDate(schedule.getScheduleDate());
                    dto.setMaxCapacity(schedule.getMaxCapacity());
                    dto.setReservedCapacity(schedule.getReservedCapacity());
                    dto.setIsUnlimitedCapacity(schedule.getIsUnlimitedCapacity());
                    dto.setStatus(schedule.getStatus().getValue());
                    dto.setConfigId(schedule.getConfigId());

                    if (schedule.getIsUnlimitedCapacity() != null && schedule.getIsUnlimitedCapacity()) {
                        dto.setAvailableCapacity(null);
                    } else if (schedule.getMaxCapacity() != null && schedule.getReservedCapacity() != null) {
                        dto.setAvailableCapacity(schedule.getMaxCapacity() - schedule.getReservedCapacity());
                    } else {
                        dto.setAvailableCapacity(0);
                    }

                    if (schedule.getTour() != null) {
                        Tour tour = schedule.getTour();
                        TourDetailsInSearchDto tourDetailsDto = new TourDetailsInSearchDto();
                        tourDetailsDto.setTourId(tour.getId());
                        tourDetailsDto.setTourName(tour.getName());
                        tourDetailsDto.setDescription(tour.getDescription());
                        tourDetailsDto.setMinAge(tour.getMinAge());
                        tourDetailsDto.setRating(tour.getRating());
                        tourDetailsDto.setProviderId(tour.getProvider().getId());
                        if (tour.getTourCategory() != null) {
                            tourDetailsDto.setCategoryName(tour.getTourCategory().getName());
                        }
                        dto.setTourDetails(tourDetailsDto);

                        // Lógica corregida para obtener la ubicación
                        List<TourAddress> tourAddresses = addressesByTourId.get(tour.getId());
                        if (tourAddresses != null && !tourAddresses.isEmpty()) {
                            TourAddress tourAddress = tourAddresses.get(0); // Tomar la primera dirección
                            TourLocationInSearchDto locationDto = new TourLocationInSearchDto();
                            locationDto.setAddress(tourAddress.getAddress());
                            locationDto.setLocation(tourAddress.getLocation());
                            locationDto.setLatitude(tourAddress.getLatitude());
                            locationDto.setLongitude(tourAddress.getLongitude());
                            if (tourAddress.getCity() != null) locationDto.setCityName(tourAddress.getCity().getName());
                            if (tourAddress.getState() != null) locationDto.setStateName(tourAddress.getState().getName());
                            if (tourAddress.getCountry() != null) locationDto.setCountryName(tourAddress.getCountry().getName());
                            dto.setLocationDetails(locationDto);
                        }
                    }

                    if (schedule.getConfig() != null && schedule.getConfig().getSlots() != null) {
                        schedule.getConfig().getSlots().stream()
                                .findFirst()
                                .ifPresent(matchingSlot -> {
                                    List<TourPriceOptionDto> priceOptions = matchingSlot.getPrices().stream()
                                            .map(price -> {
                                                TourPriceOptionDto priceDto = new TourPriceOptionDto();
                                                priceDto.setPriceId(price.getId());
                                                priceDto.setAgeType(price.getAgeType());
                                                priceDto.setMinAge(price.getMinAge());
                                                priceDto.setMaxAge(price.getMaxAge());
                                                priceDto.setPrice(price.getPrice());
                                                return priceDto;
                                            })
                                            .collect(Collectors.toList());
                                    dto.setPriceOptions(priceOptions);
                                });
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponse.<TourScheduleSearchResponseDto>builder()
                .content(responseDtos)
                .number(tourSchedulesPage.getNumber())
                .size(tourSchedulesPage.getSize())
                .totalElements(tourSchedulesPage.getTotalElements())
                .totalPages(tourSchedulesPage.getTotalPages())
                .first(tourSchedulesPage.isFirst())
                .last(tourSchedulesPage.isLast())
                .build();
    }

    @Transactional
    public List<TourScheduleBulkResponse> saveOrUpdateTourSchedules(List<TourScheduleRequest> scheduleRequests, Authentication connectedUser) {
        List<TourScheduleBulkResponse> responses = new ArrayList<>();
        for (TourScheduleRequest dto : scheduleRequests) {
            Optional<TourSchedule> existingOpt = tourScheduleRepository.findByTourIdAndScheduleDate(
                dto.getTourId(), dto.getScheduleDate()
            );

            TourScheduleConfigDto configDto = dto.getConfig();
            TourScheduleConfigResponse configResponse = null;

            TourScheduleConfigCreationRequest configRequest = new TourScheduleConfigCreationRequest();
            // Mapear los campos necesarios del DTO al request
            configRequest.setId(configDto.getId());
            configRequest.setTourId(configDto.getTourId());
            configRequest.setProviderId(configDto.getProviderId());
            configRequest.setLabel(configDto.getLabel());
            configRequest.setDaysOfWeek(configDto.getDaysOfWeek());
            configRequest.setIsUnlimitedCapacity(configDto.getIsUnlimitedCapacity());
            configRequest.setSlots(configDto.getSlots());
            configRequest.setIsTemplate(false);

            if (configDto.getId() == null) {
                configResponse = createTourScheduleConfig(configRequest, connectedUser);
            } else {
                configResponse = updateTourScheduleConfig(configDto.getId(), configRequest, connectedUser);
            }

            if (dto.getStatus() == TourScheduleStatusEnum.AVAILABLE) {
                if (existingOpt.isPresent()) {
                    TourSchedule existing = existingOpt.get();
                    if (existing.getConfigId() == null) {
                        tourScheduleRepository.delete(existing);
                    }
                    else {
                        existing.setMaxCapacity(dto.getMaxCapacity());
                        existing.setReservedCapacity(dto.getReservedCapacity());
                        existing.setIsUnlimitedCapacity(dto.getIsUnlimitedCapacity());
                        existing.setStatus(dto.getStatus());
                        if (configResponse != null) {
                            existing.setConfigId(configResponse.getId());
                        }
                        tourScheduleRepository.save(existing);
                    }
                } else {
                    TourSchedule newSchedule = new TourSchedule();
                    newSchedule.setTourId(dto.getTourId());
                    newSchedule.setScheduleDate(dto.getScheduleDate());
                    newSchedule.setMaxCapacity(dto.getMaxCapacity());
                    newSchedule.setReservedCapacity(dto.getReservedCapacity());
                    newSchedule.setIsUnlimitedCapacity(dto.getIsUnlimitedCapacity());
                    newSchedule.setStatus(dto.getStatus());
                    if (configResponse != null) {
                        newSchedule.setConfigId(configResponse.getId());
                        TourScheduleConfig config = new TourScheduleConfig();
                        config.setId(configResponse.getId());
                        newSchedule.setConfig(config);
                    }
                    tourScheduleRepository.save(newSchedule);
                }
            }

            // Construir respuesta para cada schedule procesado
            TourScheduleBulkResponse resp = new TourScheduleBulkResponse();
            resp.setTourId(dto.getTourId());
            resp.setScheduleDate(dto.getScheduleDate());
            TourScheduleConfigResponse configIdOnly = new TourScheduleConfigResponse();
            if (configResponse != null) {
                configIdOnly.setId(configResponse.getId());
            }
            resp.setConfig(configIdOnly);
            responses.add(resp);
        }
        return responses;
    }
}

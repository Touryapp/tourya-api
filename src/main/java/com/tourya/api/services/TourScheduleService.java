package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.request.TourScheduleConfigCreationRequest;
import com.tourya.api.models.request.TourScheduleConfigPriceDto;
import com.tourya.api.models.request.TourScheduleConfigSlotDto;
import com.tourya.api.models.request.TourSearchRequestDto;
import com.tourya.api.models.responses.*;
import com.tourya.api.models.specification.TourScheduleSpecification;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.TourScheduleConfigRepository;
import com.tourya.api.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourScheduleService {
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
    public TourScheduleConfigResponse createTourScheduleConfigAndGenerateSchedules(
            TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)) {
            Provider provider = providerService.findByUserAndStatusActive(user);
            // 1. Validar que el tourId existe
            Tour tour = getTour(request.getTourId(), provider.getId());

            // 2. Crear y guardar TourScheduleConfig
            TourScheduleConfig config = new TourScheduleConfig();
            config.setTourId(request.getTourId());
            config.setLabel(request.getLabel());
            config.setStartDate(request.getStartDate());
            config.setEndDate(request.getEndDate());
            // Convertir los Strings de daysOfWeek a un Set de DayOfWeek para una búsqueda eficiente
            // Manejar posibles errores si el string no es un nombre de día de la semana válido
            Set<DayOfWeek> validDaysOfWeek = request.getDaysOfWeek().stream()
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
            config.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));

            config.setIsUnlimitedCapacity(request.getIsUnlimitedCapacity() != null ? request.getIsUnlimitedCapacity() : false);

            TourScheduleConfig savedConfig = tourScheduleConfigRepository.save(config);

            // 3. Crear y guardar TourScheduleConfigSlot y TourScheduleConfigPrice
            Set<TourScheduleConfigSlot> newSlots = new HashSet<>();
            if (request.getSlots() != null) {
                for (TourScheduleConfigSlotDto slotDto : request.getSlots()) {
                    TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
                    slot.setConfig(savedConfig);
                    slot.setStartTime(slotDto.getStartTime());
                    slot.setEndTime(slotDto.getEndTime());
                    slot.setMinCapacity(slotDto.getMinCapacity());
                    slot.setMaxCapacity(slotDto.getMaxCapacity());

                    if (slotDto.getPrices() != null) {
                        Set<TourScheduleConfigPrice> newPrices = new HashSet<>();
                        for (TourScheduleConfigPriceDto priceDto : slotDto.getPrices()) {
                            TourScheduleConfigPrice price = new TourScheduleConfigPrice();
                            price.setSlot(slot); // Asociar al slot
                            price.setAgeType(priceDto.getAgeType());
                            price.setMinAge(priceDto.getMinAge());
                            price.setMaxAge(priceDto.getMaxAge());
                            price.setPrice(priceDto.getPrice());
                            newPrices.add(price);
                        }
                        slot.setPrices(newPrices);
                    }
                    newSlots.add(slot);
                }
            }
            savedConfig.setSlots(newSlots);
            tourScheduleConfigRepository.save(savedConfig);

            // 4. Generar y guardar TourSchedule para cada día aplicable
            generateAndSaveTourSchedules(savedConfig, validDaysOfWeek);
            return getTourScheduleConfigDetails(savedConfig.getId());
        }else {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
   }

    @Transactional
    public TourScheduleConfigResponse updateTourScheduleConfigAndGenerateSchedules(
            Integer configId, TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)) {
            Provider provider = providerService.findByUserAndStatusActive(user);

            // 1. Obtener la configuración existente
            TourScheduleConfig existingConfig = tourScheduleConfigRepository.findByIdWithSlots(configId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Tour configuration with ID " + configId + " not found."));

            // Validar que el tourId existe
            Tour tour = getTour(request.getTourId(), provider.getId());

            // 2. Actualizar las propiedades de la configuración principal
            existingConfig.setTourId(request.getTourId());
            existingConfig.setLabel(request.getLabel());
            existingConfig.setStartDate(request.getStartDate());
            existingConfig.setEndDate(request.getEndDate());

            // Validar y actualizar daysOfWeek
            Set<DayOfWeek> newValidDaysOfWeek = request.getDaysOfWeek().stream()
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
            existingConfig.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));

            existingConfig.setIsUnlimitedCapacity(request.getIsUnlimitedCapacity() != null ? request.getIsUnlimitedCapacity() : false);

            Map<Integer, TourScheduleConfigSlot> existingSlotsMap = existingConfig.getSlots().stream()
                    .filter(s -> s.getId() != null) // **Asegurar que solo slots con ID válido sean mapeados**
                    .collect(Collectors.toMap(TourScheduleConfigSlot::getId, Function.identity()));

            // Paso 2: Preparar una nueva colección para los slots que deben persistir (actualizados o nuevos)
            Set<TourScheduleConfigSlot> newOrUpdatedSlots = new HashSet<>();

            if (request.getSlots() != null) {
                for (TourScheduleConfigSlotDto slotDto : request.getSlots()) {
                    TourScheduleConfigSlot currentSlot;

                    if (slotDto.getId() != null && existingSlotsMap.containsKey(slotDto.getId())) {
                        currentSlot = existingSlotsMap.get(slotDto.getId());
                    } else {
                        currentSlot = new TourScheduleConfigSlot();
                        currentSlot.setConfig(existingConfig);
                    }
                    currentSlot.setStartTime(slotDto.getStartTime());
                    currentSlot.setEndTime(slotDto.getEndTime());
                    currentSlot.setMinCapacity(slotDto.getMinCapacity());
                    currentSlot.setMaxCapacity(slotDto.getMaxCapacity());

                    updateSlotPrices(currentSlot, slotDto.getPrices(), request.getCreatedBy());
                    newOrUpdatedSlots.add(currentSlot);
                }
            }

            existingConfig.getSlots().clear();
            // Paso 4: Añadir todos los slots (actualizados y nuevos) a la colección gestionada.
            existingConfig.getSlots().addAll(newOrUpdatedSlots);

            TourScheduleConfig savedConfig = tourScheduleConfigRepository.save(existingConfig);
            generateAndSaveTourSchedules(savedConfig, newValidDaysOfWeek);
            return getTourScheduleConfigDetails(savedConfig.getId());
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

    }

    private void updateSlotPrices(TourScheduleConfigSlot slot, List<TourScheduleConfigPriceDto> incomingPriceDtos, Long userId) {
        Map<Integer, TourScheduleConfigPriceDto> incomingPriceDtosById = incomingPriceDtos != null ?
                incomingPriceDtos.stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(TourScheduleConfigPriceDto::getId, Function.identity()))
                : new java.util.HashMap<>();

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

    private void generateAndSaveTourSchedules(TourScheduleConfig config, Set<DayOfWeek> validDaysOfWeek) {
        Map<String, TourSchedule> existingSchedulesMap = tourScheduleRepository.findByConfigId(config.getId())
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getScheduleDate().toString() + "_" + s.getStartTime().toString() + "_" + s.getEndTime().toString(),
                        Function.identity()
                ));
        tourScheduleRepository.deleteByConfigId(config.getId());

        List<TourSchedule> generatedSchedules = new ArrayList<>();
        LocalDate currentDate = config.getStartDate();
        while (!currentDate.isAfter(config.getEndDate())) {
            if (validDaysOfWeek.contains(currentDate.getDayOfWeek())) {
                for (TourScheduleConfigSlot slot : config.getSlots()) {
                    TourSchedule schedule = new TourSchedule();
                    schedule.setTourId(config.getTourId());
                    schedule.setScheduleDate(currentDate);
                    schedule.setStartTime(slot.getStartTime());
                    schedule.setEndTime(slot.getEndTime());
                    schedule.setIsUnlimitedCapacity(config.getIsUnlimitedCapacity());

                    if (!config.getIsUnlimitedCapacity()) {
                        schedule.setMaxCapacity(slot.getMaxCapacity());
                    } else {
                        schedule.setMaxCapacity(null);
                    }
                    String key = currentDate.toString() + "_" + slot.getStartTime().toString() + "_" + slot.getEndTime().toString();
                    if (existingSchedulesMap.containsKey(key)) {
                        TourSchedule oldSchedule = existingSchedulesMap.get(key);
                        schedule.setReservedCapacity(oldSchedule.getReservedCapacity());
                        schedule.setStatus(oldSchedule.getStatus());
                    } else {
                        schedule.setReservedCapacity(0);
                        schedule.setStatus(TourScheduleStatusEnum.AVAILABLE);
                    }

                    schedule.setConfig(config);
                    schedule.setConfigId(config.getId());
                    schedule.setCreatedBy(config.getCreatedBy());
                    schedule.setLastModifiedBy(config.getLastModifiedBy());

                    generatedSchedules.add(schedule);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        tourScheduleRepository.saveAll(generatedSchedules);
    }

    @Transactional(readOnly = true)
    public TourScheduleConfigResponse getTourScheduleConfigDetails(Integer configId) {
        TourScheduleConfig config = tourScheduleConfigRepository.findByIdWithSlots(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour configuration with ID " + configId + " not found."));

        List<TourSchedule> schedules = tourScheduleRepository.findByConfigId(configId);

        TourScheduleConfigResponse responseDto = new TourScheduleConfigResponse();
        responseDto.setId(config.getId());
        responseDto.setTourId(config.getTourId());
        responseDto.setLabel(config.getLabel());
        responseDto.setStartDate(config.getStartDate());
        responseDto.setEndDate(config.getEndDate());
        responseDto.setDaysOfWeek(config.getDaysOfWeek());
        responseDto.setIsUnlimitedCapacity(config.getIsUnlimitedCapacity());
        responseDto.setCreatedDate(config.getCreatedDate());
        responseDto.setLastModifiedDate(config.getLastModifiedDate());

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

        List<TourScheduleResponse> scheduleDtos = schedules.stream()
                .map(schedule -> {
                    TourScheduleResponse scheduleDto = new TourScheduleResponse();
                    scheduleDto.setId(schedule.getId());
                    scheduleDto.setTourId(schedule.getTourId());
                    scheduleDto.setScheduleDate(schedule.getScheduleDate());
                    scheduleDto.setStartTime(schedule.getStartTime());
                    scheduleDto.setEndTime(schedule.getEndTime());
                    scheduleDto.setMaxCapacity(schedule.getMaxCapacity());
                    scheduleDto.setReservedCapacity(schedule.getReservedCapacity());
                    scheduleDto.setIsUnlimitedCapacity(schedule.getIsUnlimitedCapacity());
                    scheduleDto.setStatus(schedule.getStatus());
                    scheduleDto.setConfigId(schedule.getConfigId());
                    return scheduleDto;
                })
                .collect(Collectors.toList());
        responseDto.setSchedules(scheduleDtos);

        return responseDto;
    }

    @Transactional(readOnly = true)
    public PageResponse<TourScheduleSearchResponseDto> searchToursForReservation(TourSearchRequestDto request) {
        Specification<TourSchedule> spec = TourScheduleSpecification.withSearchCriteria(request);
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDir()), request.getSortBy());
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.getPage(), request.getSize(), sort);
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
                    dto.setStartTime(schedule.getStartTime());
                    dto.setEndTime(schedule.getEndTime());
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
                                .filter(s -> s.getStartTime().equals(schedule.getStartTime()) && s.getEndTime().equals(schedule.getEndTime()))
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
}

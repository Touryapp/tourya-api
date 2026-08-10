package com.tourya.api.services;

import com.tourya.api._utils.PayloadIdUtils;
import com.tourya.api._utils.TouryaPriceCalculator;
import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.request.*;
import com.tourya.api.models.responses.*;
import com.tourya.api.models.specification.TourScheduleSpecification;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.TourScheduleConfigRepository;
import com.tourya.api.repository.TourScheduleConfigSlotRepository;
import com.tourya.api.repository.TourScheduleRepository;

import com.tourya.api.models.AgeRangeConfig;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourScheduleConfigGeneralService {

    private final TourScheduleRepository tourScheduleRepository;
    private final TourScheduleConfigRepository tourScheduleConfigRepository;
    private final TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;
    private final TourRepository tourRepository;
    private final ProviderService providerService;
    private final TourAddressRepository tourAddressRepository;
    private final AgeRangeConfigService ageRangeConfigService; // Servicio para obtener rangos de edad
    private final TourScheduleSlotAvailabilityService tourScheduleSlotAvailabilityService;
    private final TourScheduleOverrideService tourScheduleOverrideService;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    private Tour getTour(Integer tourId, Integer providerId) {
        Tour tour = tourRepository.findTourByIdAndProviderId(tourId, providerId);
        if (tour != null) {
            return tour;
        } else {
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }

    @Transactional
    public TourScheduleConfigResponse createTourScheduleConfig(
            TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        Provider provider = providerService.findByUserAndStatusActive(user);

        boolean isTemplate = Boolean.TRUE.equals(request.getIsTemplate());

        if (!isTemplate && request.getTourId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tourId is required when isTemplate is false.");
        }

        Tour tour = null;
        if (request.getTourId() != null) {
            tour = getTour(request.getTourId(), provider.getId());
            requireTourAcceptedForProviderSchedule(tour, roleList);
        }

        // 1. Construir el grafo de entidades a partir del DTO
        TourScheduleConfig config = buildConfigFromRequest(request, provider, tour);
        Set<TourScheduleConfigSlot> slots = buildSlotsAndPricesFromRequest(request.getSlots(), config, roleList);
        config.setSlots(slots);

        // 2. Guardar la configuración completa
        TourScheduleConfig savedConfig = tourScheduleConfigRepository.save(config);

        recalculateAvailabilityForAllSlots(savedConfig);
        TourScheduleConfig refreshed = tourScheduleConfigRepository.findByIdWithSlots(savedConfig.getId())
                .orElse(savedConfig);

        return mapToTourScheduleConfigResponse(refreshed, roleList);
    }

    private TourScheduleConfig buildConfigFromRequest(TourScheduleConfigCreationRequest request, Provider provider,
            Tour tour) {
        TourScheduleConfig config = new TourScheduleConfig();
        config.setLabel(request.getLabel());
        config.setProvider(provider);
        config.setProviderId(provider.getId());
        if (tour != null) {
            config.setTour(tour);
            config.setTourId(tour.getId());
        } else {
            config.setTourId(null);
        }

        config.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));
        config.setIsTemplate(request.getIsTemplate());
        return config;
    }

    private Set<TourScheduleConfigSlot> buildSlotsAndPricesFromRequest(Set<TourScheduleConfigSlotDto> slotDtos,
            TourScheduleConfig config, List<Role> roleList) {
        if (slotDtos == null) {
            return new HashSet<>();
        }
        Tour tourForConfig = config.getTourId() != null
                ? tourRepository.findById(config.getTourId()).orElse(null)
                : null;

        Set<TourScheduleConfigSlot> slots = new HashSet<>();
        for (TourScheduleConfigSlotDto slotDto : slotDtos) {
            validateSlotPrices(new HashSet<>(slotDto.getPrices()), roleList);

            TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
            slot.setConfig(config);
            slot.setStartTime(slotDto.getStartTime());
            slot.setEndTime(slotDto.getEndTime());
            slot.setCapacity(slotDto.getCapacity());
            slot.setBookings(0);
            int cap0 = slotDto.getCapacity() != null ? slotDto.getCapacity() : 0;
            slot.setAvailability(cap0);
            tourScheduleSlotAvailabilityService.applyMinCapacityAndCheckAvailability(slot, tourForConfig);

            // BE-02 (RN-015): heredar el porcentaje default del tour para nuevos slots.
            // Antes se seteaba en ZERO y el ADMIN tenia que llamar despues a
            // PUT /tour-schedules/tours/{tourId}/percentage. Ahora nace ya con el valor
            // del tour; el ADMIN puede sobrescribirlo por rango de fechas via override.
            BigDecimal tourDefaultPct = tourForConfig != null && tourForConfig.getPorcentajeTourya() != null
                    ? tourForConfig.getPorcentajeTourya()
                    : BigDecimal.ZERO;
            slot.setSlotPorcentajeTourya(tourDefaultPct);

            if (slotDto.getPrices() != null) {
                Set<TourScheduleConfigPrice> prices = new HashSet<>();
                for (TourScheduleConfigPriceDto priceDto : slotDto.getPrices()) {
                    TourScheduleConfigPrice price = buildPriceEntity(slot, priceDto, tourDefaultPct, roleList);
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
                                "Invalid day of the week: " + dayStr
                                        + ". It must be one of the java.time.DayOfWeek values (e.g., MONDAY).");
                    }
                })
                .collect(Collectors.toSet());
    }

    @Transactional
    public TourScheduleConfigResponse updateTourScheduleConfig(
            Integer configId, TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        providerService.findByUserAndStatusActive(user);

        // 1. Obtener la configuración existente
        TourScheduleConfig existingConfig = tourScheduleConfigRepository.findByIdWithSlots(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour configuration with ID " + configId + " not found."));
        if (existingConfig.getTourId() != null) {
            Tour tour = tourRepository.findById(existingConfig.getTourId()).orElse(null);
            requireTourAcceptedForProviderSchedule(tour, roleList);
        }

        // 3. Actualizar las propiedades y colecciones de la entidad
        updateConfigProperties(existingConfig, request);
        manageSlotsUpdate(existingConfig, request.getSlots(), roleList);

        // 4. Guardar y flush antes de recalcular (evita TransientObjectException al aplicar plantilla)
        TourScheduleConfig savedConfig = tourScheduleConfigRepository.saveAndFlush(existingConfig);

        // 5. Recargar desde BD; no recalcular sobre la entidad en memoria con slots huérfanos/transient
        TourScheduleConfig refreshed = tourScheduleConfigRepository.findByIdWithSlots(savedConfig.getId())
                .orElse(savedConfig);
        recalculateAvailabilityForAllSlots(refreshed);

        // Validar días de la semana (lanza 400 si son inválidos)
        getValidDaysOfWeek(request.getDaysOfWeek());

        return mapToTourScheduleConfigResponse(refreshed, roleList);
    }

    /**
     * Recalcula {@code bookings} y {@code availability} de cada slot a partir de reservas/carrito,
     * no solo {@code capacity - bookings} en memoria (que puede estar desactualizado).
     */
    private void recalculateAvailabilityForAllSlots(TourScheduleConfig config) {
        if (config == null || config.getSlots() == null) {
            return;
        }
        List<Integer> slotIds = config.getSlots().stream()
                .map(TourScheduleConfigSlot::getId)
                .filter(PayloadIdUtils::isPersistedId)
                .toList();
        for (Integer slotId : slotIds) {
            // TC-019 (#231): usar variante REQUIRED — el batch acaba de INSERTar los slots
            // en esta misma tx y aun no commiteo. REQUIRES_NEW abriria una tx nueva con
            // connection distinta que no los ve (READ COMMITTED) -> "Slot not found" -> 404.
            tourScheduleSlotAvailabilityService.recalculateInSameTransaction(slotId);
        }
    }

    private void updateConfigProperties(TourScheduleConfig existingConfig, TourScheduleConfigCreationRequest request) {
        existingConfig.setLabel(request.getLabel());
        existingConfig.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));
        existingConfig.setIsTemplate(request.getIsTemplate()); // <-- Mapear isTemplate
    }

    /**
     * Actualiza slots de la config. Ids de slot/precio que no pertenecen a esta config
     * (p. ej. copiados desde una plantilla) se ignoran: se matchea por horario o se crea nuevo.
     */
    private void manageSlotsUpdate(TourScheduleConfig existingConfig, Set<TourScheduleConfigSlotDto> requestedSlots,
            List<Role> roleList) {
        if (requestedSlots == null || requestedSlots.isEmpty()) {
            return;
        }

        Map<Integer, TourScheduleConfigSlot> existingSlotsMap = existingConfig.getSlots().stream()
                .filter(s -> PayloadIdUtils.isPersistedId(s.getId()))
                .collect(Collectors.toMap(TourScheduleConfigSlot::getId, Function.identity()));

        Set<TourScheduleConfigSlot> handledSlots =
                Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Integer> handledSlotIds = new HashSet<>();
        Tour tourForConfig = existingConfig.getTourId() != null
                ? tourRepository.findById(existingConfig.getTourId()).orElse(null)
                : null;

        for (TourScheduleConfigSlotDto slotDto : requestedSlots) {
            Integer requestedSlotId = PayloadIdUtils.normalizeId(slotDto.getId());
            TourScheduleConfigSlot currentSlot;
            boolean isNewSlot;

            if (PayloadIdUtils.isPersistedId(requestedSlotId)
                    && existingSlotsMap.containsKey(requestedSlotId)) {
                currentSlot = existingSlotsMap.get(requestedSlotId);
                isNewSlot = false;
            } else {
                currentSlot = findExistingSlotByTime(existingConfig, slotDto, handledSlotIds);
                if (currentSlot != null) {
                    isNewSlot = false;
                } else {
                    currentSlot = new TourScheduleConfigSlot();
                    currentSlot.setConfig(existingConfig);
                    currentSlot.setBookings(0);
                    currentSlot.setAvailability(0);
                    isNewSlot = true;
                    existingConfig.getSlots().add(currentSlot);
                }
            }

            if (currentSlot.getId() != null) {
                handledSlotIds.add(currentSlot.getId());
            }
            handledSlots.add(currentSlot);

            currentSlot.setStartTime(slotDto.getStartTime());
            currentSlot.setEndTime(slotDto.getEndTime());
            currentSlot.setCapacity(slotDto.getCapacity());
            if (currentSlot.getBookings() == null) {
                currentSlot.setBookings(0);
            }
            int booked = currentSlot.getBookings();
            Integer capVal = currentSlot.getCapacity();
            currentSlot.setAvailability(capVal != null ? Math.max(0, capVal - booked) : 0);
            tourScheduleSlotAvailabilityService.applyMinCapacityAndCheckAvailability(currentSlot, tourForConfig);

            // BE-02 (RN-015): para slots nuevos, heredar el porcentaje default del tour.
            // Slots existentes conservan su slot_porcentaje_tourya actual (posiblemente
            // override manual del ADMIN).
            BigDecimal tourDefaultPct = tourForConfig != null && tourForConfig.getPorcentajeTourya() != null
                    ? tourForConfig.getPorcentajeTourya()
                    : BigDecimal.ZERO;
            BigDecimal slotPct = isNewSlot
                    ? tourDefaultPct
                    : TouryaPriceCalculator.normalizePercentage(currentSlot.getSlotPorcentajeTourya());
            if (isNewSlot) {
                currentSlot.setSlotPorcentajeTourya(tourDefaultPct);
            }

            updateSlotPrices(currentSlot, new HashSet<>(slotDto.getPrices()), slotPct, roleList);
        }

        // Quitar solo slots que no vinieron en el request (reemplazo total). No usar clear()+addAll:
        // con orphanRemoval Hibernate marca como huérfanos slots que se reutilizan y revienta en flush.
        existingConfig.getSlots().removeIf(slot -> !handledSlots.contains(slot));
    }

    private TourScheduleConfigSlot findExistingSlotByTime(
            TourScheduleConfig config,
            TourScheduleConfigSlotDto slotDto,
            Set<Integer> alreadyHandled) {
        if (slotDto.getStartTime() == null || slotDto.getEndTime() == null || config.getSlots() == null) {
            return null;
        }
        return config.getSlots().stream()
                .filter(s -> s.getStartTime() != null
                        && s.getEndTime() != null
                        && s.getStartTime().equals(slotDto.getStartTime())
                        && s.getEndTime().equals(slotDto.getEndTime())
                        && (s.getId() == null || !alreadyHandled.contains(s.getId())))
                .findFirst()
                .orElse(null);
    }

    private void updateSlotPrices(TourScheduleConfigSlot slot, Set<TourScheduleConfigPriceDto> incomingPriceDtos,
            BigDecimal slotPorcentajeTourya, List<Role> roleList) {
        validateSlotPrices(incomingPriceDtos, roleList);

        if (incomingPriceDtos == null) {
            incomingPriceDtos = Collections.emptySet();
        }
        if (slot.getPrices() == null) {
            slot.setPrices(new HashSet<>());
        }

        Set<Integer> incomingPriceIds = incomingPriceDtos.stream()
                .map(TourScheduleConfigPriceDto::getId)
                .map(PayloadIdUtils::normalizeId)
                .filter(Objects::nonNull)
                .filter(id -> slot.getPrices().stream().anyMatch(p -> Objects.equals(p.getId(), id)))
                .collect(Collectors.toSet());
        Set<AgePriceType> incomingAgeTypes = incomingPriceDtos.stream()
                .map(TourScheduleConfigPriceDto::getAgeType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<TourScheduleConfigPrice> pricesToDelete = slot.getPrices().stream()
                .filter(existing -> !isIncomingPriceMatch(existing, incomingPriceIds, incomingAgeTypes))
                .collect(Collectors.toList());
        slot.getPrices().removeAll(pricesToDelete);

        for (TourScheduleConfigPriceDto priceDto : incomingPriceDtos) {
            TourScheduleConfigPrice currentPrice = findExistingPrice(slot, priceDto);
            if (currentPrice == null) {
                currentPrice = new TourScheduleConfigPrice();
                currentPrice.setSlot(slot);
                slot.getPrices().add(currentPrice);
            }
            applyPriceDtoToEntity(currentPrice, priceDto, slotPorcentajeTourya, roleList);
        }
    }

    private static boolean isIncomingPriceMatch(TourScheduleConfigPrice existing, Set<Integer> incomingPriceIds,
            Set<AgePriceType> incomingAgeTypes) {
        if (existing.getId() != null && incomingPriceIds.contains(existing.getId())) {
            return true;
        }
        return existing.getAgeType() != null && incomingAgeTypes.contains(existing.getAgeType());
    }

    private static TourScheduleConfigPrice findExistingPrice(TourScheduleConfigSlot slot,
            TourScheduleConfigPriceDto priceDto) {
        Integer requestedPriceId = PayloadIdUtils.normalizeId(priceDto.getId());
        if (PayloadIdUtils.isPersistedId(requestedPriceId)) {
            TourScheduleConfigPrice byId = slot.getPrices().stream()
                    .filter(p -> Objects.equals(p.getId(), requestedPriceId))
                    .findFirst()
                    .orElse(null);
            if (byId != null) {
                return byId;
            }
        }
        if (priceDto.getAgeType() == null) {
            return null;
        }
        return slot.getPrices().stream()
                .filter(p -> priceDto.getAgeType() == p.getAgeType())
                .findFirst()
                .orElse(null);
    }

    /** El front a veces envía solo {@code price}; para proveedor se usa como {@code providerPrice}. */
    private void normalizeProviderPriceDto(TourScheduleConfigPriceDto priceDto) {
        if (priceDto != null && priceDto.getProviderPrice() == null && priceDto.getPrice() != null) {
            priceDto.setProviderPrice(priceDto.getPrice());
        }
    }

    private void validateSlotPrices(Set<TourScheduleConfigPriceDto> prices, List<Role> roleList) {
        if (prices == null || prices.isEmpty()) {
            return;
        }

        Set<AgePriceType> existingAgeTypes = new HashSet<>();
        for (TourScheduleConfigPriceDto priceDto : prices) {
            normalizeProviderPriceDto(priceDto);
            if (!existingAgeTypes.add(priceDto.getAgeType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate ageType found for the same slot: " + priceDto.getAgeType());
            }
            if (!Utils.isTouryaBackoffice(roleList)
                    && priceDto.getProviderPrice() == null
                    && priceDto.getPrice() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "providerPrice is required for ageType: " + priceDto.getAgeType());
            }
        }
    }

    private TourScheduleConfigPrice buildPriceEntity(TourScheduleConfigSlot slot,
            TourScheduleConfigPriceDto priceDto, BigDecimal slotPorcentajeTourya, List<Role> roleList) {
        TourScheduleConfigPrice price = new TourScheduleConfigPrice();
        price.setSlot(slot);
        applyPriceDtoToEntity(price, priceDto, slotPorcentajeTourya, roleList);
        return price;
    }

    private void applyPriceDtoToEntity(TourScheduleConfigPrice price, TourScheduleConfigPriceDto priceDto,
            BigDecimal slotPorcentajeTourya, List<Role> roleList) {
        normalizeProviderPriceDto(priceDto);
        price.setAgeType(priceDto.getAgeType());
        if (Utils.isTouryaBackoffice(roleList)) {
            price.setProviderPrice(priceDto.getProviderPrice());
            if (priceDto.getPrice() != null) {
                price.setPrice(priceDto.getPrice());
            } else if (priceDto.getProviderPrice() != null) {
                price.setPrice(TouryaPriceCalculator.calculateSalePrice(
                        priceDto.getProviderPrice(), slotPorcentajeTourya));
            }
            return;
        }
        if (priceDto.getProviderPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "providerPrice is required for ageType: " + priceDto.getAgeType());
        }
        price.setProviderPrice(priceDto.getProviderPrice());
        price.setPrice(TouryaPriceCalculator.calculateSalePrice(
                priceDto.getProviderPrice(), slotPorcentajeTourya));
    }

    @Transactional(readOnly = true)
    public TourScheduleConfigResponse getTourScheduleConfigDetails(Integer configId) {
        TourScheduleConfig config = tourScheduleConfigRepository.findByIdWithSlots(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour configuration with ID " + configId + " not found."));

        tourScheduleRepository.findByConfigId(configId);
        return mapToTourScheduleConfigResponse(config, null);
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

    // 2. Consulta de todos los templates de un proveedor (Opción procedimiento
    // almacenado)
    // Si decides usar un procedimiento almacenado, crea el método en el repositorio
    // y llama aquí:
    // public List<TourScheduleConfigResponse> getTemplatesByProviderSP(Integer
    // providerId) {
    // List<TourScheduleConfig> templates =
    // tourScheduleConfigRepository.findTemplatesByProviderSP(providerId);
    // return templates.stream()
    // .map(this::mapToTourScheduleConfigResponse)
    // .collect(Collectors.toList());
    // }

    /*
     * // Consulta de todos los templates de un proveedor usando procedimiento
     * almacenado
     * 
     * @Transactional(readOnly = true)
     * public List<TourScheduleConfigResponse> getTemplatesByProviderSP(Integer
     * providerId) {
     * List<Object[]> rows =
     * tourScheduleConfigRepository.findTemplatesByProviderSP(providerId);
     * Map<Integer, TourScheduleConfigResponse> configMap = new HashMap<>();
     * 
     * for (Object[] row : rows) {
     * Integer configId = (Integer) row[0];
     * TourScheduleConfigResponse configDto = configMap.computeIfAbsent(configId, id
     * -> {
     * TourScheduleConfigResponse dto = new TourScheduleConfigResponse();
     * dto.setId(configId);
     * dto.setTourId((Integer) row[1]);
     * dto.setLabel((String) row[2]);
     * dto.setStartDate((LocalDate) row[3]);
     * dto.setEndDate((LocalDate) row[4]);
     * dto.setDaysOfWeek(row[5] != null ? Arrays.asList((String[]) row[5]) : null);
     * dto.setIsUnlimitedCapacity((Boolean) row[6]);
     * dto.setCreatedDate((java.sql.Timestamp) row[8]);
     * dto.setLastModifiedDate((java.sql.Timestamp) row[9]);
     * dto.setProviderId((Integer) row[10]);
     * dto.setIsTemplate((Boolean) row[11]);
     * dto.setSlots(new HashSet<>());
     * return dto;
     * });
     * 
     * // Slot
     * Integer slotId = (Integer) row[12];
     * if (slotId != null) {
     * TourScheduleSlotResponse slotDto = configDto.getSlots().stream()
     * .filter(s -> s.getId().equals(slotId))
     * .findFirst()
     * .orElseGet(() -> {
     * TourScheduleSlotResponse s = new TourScheduleSlotResponse();
     * s.setId(slotId);
     * s.setStartTime((java.sql.Time) row[13]);
     * s.setEndTime((java.sql.Time) row[14]);
     * s.setMinCapacity((Integer) row[15]);
     * s.setMaxCapacity((Integer) row[16]);
     * s.setPrices(new HashSet<>());
     * configDto.getSlots().add(s);
     * return s;
     * });
     * 
     * // Price
     * Integer priceId = (Integer) row[17];
     * if (priceId != null) {
     * TourSchedulePriceResponse priceDto = new TourSchedulePriceResponse();
     * priceDto.setId(priceId);
     * priceDto.setAgeType(row[18] != null ? AgePriceType.valueOf((String) row[18])
     * : null);
     * priceDto.setMinAge((Integer) row[19]);
     * priceDto.setMaxAge((Integer) row[20]);
     * priceDto.setPrice(row[21] != null ? new
     * java.math.BigDecimal(row[21].toString()) : null);
     * slotDto.getPrices().add(priceDto);
     * }
     * }
     * }
     * return new ArrayList<>(configMap.values());
     * }
     */

    private TourScheduleConfigResponse mapToTourScheduleConfigResponse(TourScheduleConfig config, List<Role> roleList) {
        return mapToTourScheduleConfigResponse(config, roleList, ageRangeConfigService.getAllAsMap());
    }

    private TourScheduleConfigResponse mapToTourScheduleConfigResponse(TourScheduleConfig config, List<Role> roleList,
            Map<AgePriceType, AgeRangeConfig> ageConfigMap) {
        return mapToTourScheduleConfigResponse(config, roleList, ageConfigMap, true, null);
    }

    private TourScheduleConfigResponse mapToTourScheduleConfigResponse(TourScheduleConfig config, List<Role> roleList,
            Map<AgePriceType, AgeRangeConfig> ageConfigMap, boolean includeConfigSlotPercentage) {
        return mapToTourScheduleConfigResponse(config, roleList, ageConfigMap, includeConfigSlotPercentage, null);
    }

    /**
     * TC-004: {@code scheduleDate} opcional. Si se pasa, cada slot expone bookings/availability
     * calculados por (slot_id, scheduleDate) en runtime. Si es null se mantiene el valor
     * denormalizado del slot config (comportamiento pre-TC-004, apropiado para vistas de
     * edición de la config donde no hay una fecha específica).
     */
    private TourScheduleConfigResponse mapToTourScheduleConfigResponse(TourScheduleConfig config, List<Role> roleList,
            Map<AgePriceType, AgeRangeConfig> ageConfigMap, boolean includeConfigSlotPercentage,
            LocalDate scheduleDate) {
        boolean showTouryaFields = roleList != null && Utils.isTouryaBackoffice(roleList);

        TourScheduleConfigResponse responseDto = new TourScheduleConfigResponse();
        responseDto.setId(config.getId());
        responseDto.setProviderId(config.getProviderId());
        responseDto.setLabel(config.getLabel());
        responseDto.setDaysOfWeek(config.getDaysOfWeek());

        Set<TourScheduleSlotResponse> slotDtos = config.getSlots().stream()
                .map(slot -> mapSlotToResponse(slot, ageConfigMap, showTouryaFields, includeConfigSlotPercentage, scheduleDate))
                .collect(Collectors.toSet());
        responseDto.setSlots(slotDtos);
        return responseDto;
    }

    private TourScheduleSlotResponse mapSlotToResponse(TourScheduleConfigSlot slot,
            Map<AgePriceType, AgeRangeConfig> ageConfigMap, boolean showTouryaFields) {
        return mapSlotToResponse(slot, ageConfigMap, showTouryaFields, true, null);
    }

    private TourScheduleSlotResponse mapSlotToResponse(TourScheduleConfigSlot slot,
            Map<AgePriceType, AgeRangeConfig> ageConfigMap, boolean showTouryaFields,
            boolean includeConfigSlotPercentage,
            LocalDate scheduleDate) {
        TourScheduleSlotResponse slotDto = new TourScheduleSlotResponse();
        slotDto.setId(slot.getId());
        slotDto.setStartTime(slot.getStartTime());
        slotDto.setEndTime(slot.getEndTime());
        slotDto.setCapacity(slot.getCapacity());
        // TC-004: si viene fecha, contar (slot_id, scheduleDate) en runtime. Si no, mantener denormalizado.
        if (scheduleDate != null && slot.getId() != null) {
            int bookings = tourScheduleSlotAvailabilityService.countBookingsForSlotOnDate(slot.getId(), scheduleDate);
            int availability = slot.getCapacity() != null
                    ? Math.max(0, slot.getCapacity() - bookings)
                    : 0;
            slotDto.setBookings(bookings);
            slotDto.setAvailability(availability);
        } else {
            slotDto.setBookings(slot.getBookings());
            slotDto.setAvailability(slot.getAvailability());
        }
        slotDto.setMinCapacityCalc(slot.getMinCapacityCalc());
        slotDto.setCheckAvailability(slot.getCheckAvailability());
        if (showTouryaFields && includeConfigSlotPercentage) {
            slotDto.setSlotPorcentajeTourya(
                    TouryaPriceCalculator.toApiPercentPoints(slot.getSlotPorcentajeTourya()));
        }

        Set<TourSchedulePriceResponse> priceDtos = slot.getPrices().stream()
                .map(price -> {
                    TourSchedulePriceResponse priceDto = new TourSchedulePriceResponse();
                    priceDto.setId(price.getId());
                    priceDto.setAgeType(price.getAgeType());
                    AgeRangeConfig ageConfig = ageConfigMap.get(price.getAgeType());
                    if (ageConfig != null) {
                        priceDto.setMinAge(ageConfig.getMinAge());
                        priceDto.setMaxAge(ageConfig.getMaxAge());
                    } else {
                        priceDto.setMinAge(0);
                        priceDto.setMaxAge(0);
                    }
                    // TC-008 (RN-019 actualizada 2026-07-23): el PROVIDER ya no ve el price cliente,
                    // solo su providerPrice. showTouryaFields = ADMIN o BACKOFFICE_OPERATION.
                    if (showTouryaFields) {
                        priceDto.setPrice(price.getPrice());
                    }
                    priceDto.setProviderPrice(price.getProviderPrice());
                    return priceDto;
                })
                .collect(Collectors.toSet());
        slotDto.setPrices(priceDtos);
        return slotDto;
    }

    private TourScheduleConfigResponse mapToTourScheduleConfigResponse(TourScheduleConfig config) {
        return mapToTourScheduleConfigResponse(config, null);
    }

    private TourScheduleConfigResponse convertToTourScheduleConfigResponse(TourScheduleConfig config,
            List<Role> roleList) {
        return mapToTourScheduleConfigResponse(config, roleList);
    }

    @Transactional(readOnly = true)
    public List<TourScheduleResponse> findAllByTourId(Integer tourId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        List<Role> roleList = user.getRoles();
        
        Tour tour;
        if (Utils.isTouryaBackoffice(roleList)) {
            tour = tourRepository.findById(tourId)
                    .orElseThrow(() -> new ResourceNotFoundException("No tour with this id was found."));
        } else if (Utils.isProviderSide(roleList)) {
            Provider provider = providerService.findByUserAndStatusActive(user);
            tour = getTour(tourId, provider.getId());
        } else {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

        List<TourSchedule> tourSchedules = tourScheduleRepository.findByTourId(tour.getId());
        if (tourSchedules.isEmpty()) {
            return List.of();
        }

        Set<Integer> configIds = tourSchedules.stream()
                .map(TourSchedule::getConfigId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, TourScheduleConfig> configById = configIds.isEmpty()
                ? Map.of()
                : tourScheduleConfigRepository.findByIdInWithSlots(configIds).stream()
                        .collect(Collectors.toMap(TourScheduleConfig::getId, Function.identity()));

        List<Integer> scheduleIds = tourSchedules.stream()
                .map(TourSchedule::getId)
                .collect(Collectors.toList());
        TourScheduleOverrideService.OverrideBatchData overrideBatch =
                tourScheduleOverrideService.loadOverrideBatch(scheduleIds);

        Map<AgePriceType, AgeRangeConfig> ageConfigMap = ageRangeConfigService.getAllAsMap();
        boolean showSlotPercentage = roleList != null && Utils.isTouryaBackoffice(roleList);

        return tourSchedules.stream()
                .map(schedule -> {
                    TourScheduleResponse dto = new TourScheduleResponse();
                    dto.setId(schedule.getId());
                    dto.setTourId(schedule.getTourId());
                    dto.setScheduleDate(schedule.getScheduleDate());
                    dto.setStatus(schedule.getStatus());
                    dto.setConfigId(schedule.getConfigId());
                    if (schedule.getConfigId() != null) {
                        TourScheduleConfig config = configById.get(schedule.getConfigId());
                        if (config != null) {
                            // TC-004: pasar la fecha del schedule para que los slots reporten bookings/availability por-día.
                            TourScheduleConfigResponse configResponse =
                                    mapToTourScheduleConfigResponse(config, roleList, ageConfigMap, false, schedule.getScheduleDate());
                            tourScheduleOverrideService.applyToConfigResponse(
                                    schedule.getId(),
                                    configResponse,
                                    overrideBatch,
                                    showSlotPercentage);
                            dto.setConfig(configResponse);
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public UpdateSlotPercentageResultResponse updateSlotPercentageByDateRange(
            Integer tourId,
            UpdateSlotPercentageRangeRequest request,
            Authentication connectedUser) {
        requireTouryaBackoffice(connectedUser);
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
        }
        if (!tourRepository.existsById(tourId)) {
            throw new ResourceNotFoundException("Tour not found with id = " + tourId);
        }
        BigDecimal fraction = TouryaPriceCalculator.fromApiPercentPoints(request.getSlotPercentageTourya());
        List<TourSchedule> schedules = tourScheduleRepository.findByTourIdAndScheduleDateBetween(
                tourId, request.getStartDate(), request.getEndDate());

        int schedulesProcessed = 0;
        int slotsUpdated = 0;
        int pricesRecalculated = 0;

        for (TourSchedule schedule : schedules) {
            if (schedule.getConfigId() == null) {
                continue;
            }
            TourScheduleConfig config = tourScheduleConfigRepository.findByIdWithSlots(schedule.getConfigId())
                    .orElse(null);
            if (config == null || config.getSlots() == null || config.getSlots().isEmpty()) {
                continue;
            }
            schedulesProcessed++;
            for (TourScheduleConfigSlot slot : config.getSlots()) {
                slotsUpdated++;
            }
            pricesRecalculated += tourScheduleOverrideService.applyPercentageToScheduleSlots(
                    schedule.getId(), config.getSlots(), fraction);
        }

        return UpdateSlotPercentageResultResponse.builder()
                .tourId(tourId)
                .savedSlotPercentageTourya(request.getSlotPercentageTourya())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .schedulesProcessed(schedulesProcessed)
                .slotsUpdated(slotsUpdated)
                .pricesRecalculated(pricesRecalculated)
                .build();
    }

    @Transactional
    public UpdateSlotPercentageResultResponse updateSlotPercentageForSlot(
            Integer tourId,
            Integer slotId,
            UpdateSlotPercentageSingleRequest request,
            Authentication connectedUser) {
        requireTouryaBackoffice(connectedUser);
        BigDecimal fraction = TouryaPriceCalculator.fromApiPercentPoints(request.getSlotPercentageTourya());

        TourSchedule schedule = tourScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Schedule not found with id = " + request.getScheduleId()));
        if (!tourId.equals(schedule.getTourId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Schedule " + request.getScheduleId() + " does not belong to tour " + tourId);
        }

        TourScheduleConfigSlot slot = tourScheduleConfigSlotRepository.findByIdAndTourIdWithPrices(slotId, tourId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot not found for tour. tourId=" + tourId + ", slotId=" + slotId));

        if (schedule.getConfigId() == null || !schedule.getConfigId().equals(slot.getConfigId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Slot " + slotId + " is not part of the config for schedule " + schedule.getId());
        }

        int pricesUpdated = tourScheduleOverrideService.applyPercentageToScheduleSlot(
                schedule.getId(), slot, fraction);

        return UpdateSlotPercentageResultResponse.builder()
                .tourId(tourId)
                .savedSlotPercentageTourya(request.getSlotPercentageTourya())
                .scheduleId(schedule.getId())
                .schedulesProcessed(1)
                .slotsUpdated(1)
                .pricesRecalculated(pricesUpdated)
                .build();
    }

    private void requireTouryaBackoffice(Authentication connectedUser) {
        if (Utils.isTouryaBackoffice(connectedUser)) {
            return;
        }
        String roles = connectedUser != null
                ? connectedUser.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("(ninguno)")
                : "(sin autenticación)";
        throw new InsufficientPrivilegesException(
                NOT_PRIVILEGES + " Se requiere ADMIN o BACKOFFICE_OPERATION. Roles actuales: " + roles);
    }

    /** Proveedor solo configura horarios si el tour ya está publicado (ACCEPTED). */
    private void requireTourAcceptedForProviderSchedule(Tour tour, List<Role> roles) {
        if (tour == null || Utils.isTouryaBackoffice(roles)) {
            return;
        }
        if (Utils.isProviderSide(roles) && tour.getStatus() != TourStatusEnum.ACCEPTED) {
            throw new OperationNotPermittedException(
                    "Solo puede configurar horarios cuando el tour está en estado ACCEPTED");
        }
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

        // Paso 2: Cargar eficientemente las direcciones para todos los tours de la
        // página.
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
                    dto.setStatus(schedule.getStatus().getValue());
                    dto.setConfigId(schedule.getConfigId());

                    if (schedule.getTour() != null) {
                        Tour tour = schedule.getTour();
                        TourDetailsInSearchDto tourDetailsDto = new TourDetailsInSearchDto();
                        tourDetailsDto.setTourId(tour.getId());
                        tourDetailsDto.setTourName(tour.getName() != null ? tour.getName().getEs() : null);
                        tourDetailsDto
                                .setDescription(tour.getDescription() != null ? tour.getDescription().getEs() : null);
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
                            // Extraer el valor en español del TranslatedField
                            locationDto.setLocation(
                                    tourAddress.getLocation() != null ? tourAddress.getLocation().getEs() : null);
                            locationDto.setLatitude(tourAddress.getLatitude());
                            locationDto.setLongitude(tourAddress.getLongitude());
                            if (tourAddress.getCity() != null)
                                locationDto.setCityName(tourAddress.getCity().getName());
                            if (tourAddress.getState() != null)
                                locationDto.setStateName(tourAddress.getState().getName());
                            if (tourAddress.getCountry() != null)
                                locationDto.setCountryName(tourAddress.getCountry().getName());
                            dto.setLocationDetails(locationDto);
                        }
                    }

                    if (schedule.getConfig() != null && schedule.getConfig().getSlots() != null) {
                        // Obtener mapa de configuraciones
                        Map<AgePriceType, AgeRangeConfig> ageConfigMap = ageRangeConfigService.getAllAsMap();

                        schedule.getConfig().getSlots().stream()
                                .findFirst()
                                .ifPresent(matchingSlot -> {
                                    List<TourPriceOptionDto> priceOptions = matchingSlot.getPrices().stream()
                                            .map(price -> {
                                                TourPriceOptionDto priceDto = new TourPriceOptionDto();
                                                priceDto.setPriceId(price.getId());
                                                priceDto.setAgeType(price.getAgeType());

                                                AgeRangeConfig ageConfig = ageConfigMap.get(price.getAgeType());
                                                if (ageConfig != null) {
                                                    priceDto.setMinAge(ageConfig.getMinAge());
                                                    priceDto.setMaxAge(ageConfig.getMaxAge());
                                                } else {
                                                    priceDto.setMinAge(0);
                                                    priceDto.setMaxAge(0);
                                                }

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
    public List<TourScheduleBulkResponse> saveOrUpdateTourSchedules(List<TourScheduleRequest> scheduleRequests,
            Authentication connectedUser) {
        List<TourScheduleBulkResponse> responses = new ArrayList<>();
        BulkConfigState bulkConfigState = new BulkConfigState(scheduleRequests);

        for (TourScheduleRequest dto : scheduleRequests) {
            Tour tourForSchedule = tourRepository.findById(dto.getTourId()).orElse(null);
            requireTourAcceptedForProviderSchedule(tourForSchedule,
                    ((User) connectedUser.getPrincipal()).getRoles());

            Optional<TourSchedule> existingScheduleOpt = tourScheduleRepository.findByTourIdAndScheduleDate(
                    dto.getTourId(),
                    dto.getScheduleDate());

            TourScheduleConfig config;
            TourSchedule schedule;

            if (existingScheduleOpt.isPresent()) {
                schedule = existingScheduleOpt.get();
                config = resolveConfigForBulk(
                        dto.getConfig(), dto.getTourId(), schedule.getConfigId(), connectedUser, bulkConfigState);
                schedule.setConfig(config);
                updateScheduleProperties(schedule, dto);
            } else {
                config = resolveConfigForBulk(dto.getConfig(), dto.getTourId(), null, connectedUser, bulkConfigState);
                schedule = createScheduleFromDto(dto, config);
            }

            TourSchedule savedSchedule = tourScheduleRepository.save(schedule);
            tourScheduleRepository.flush();

            responses.add(buildBulkResponse(dto, savedSchedule, config, connectedUser));
        }

        return responses;
    }

    /**
     * Estado compartido del batch: evita crear/actualizar la misma config varias veces y clona cuando
     * el id del DTO apunta a una config ya usada por fechas fuera de este batch (p. ej. mes anterior).
     */
    private static final class BulkConfigState {
        private final Set<LocalDate> batchDates;
        private final Set<Integer> configsHandledInBatch = new HashSet<>();
        private final Map<Integer, Integer> configsCreatedByTour = new HashMap<>();
        private final Map<Integer, Integer> configRemapInBatch = new HashMap<>();

        private BulkConfigState(List<TourScheduleRequest> scheduleRequests) {
            this.batchDates = scheduleRequests.stream()
                    .map(TourScheduleRequest::getScheduleDate)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Reutiliza una config existente cuando el DTO trae {@code config.id}; crea una sola config nueva
     * por batch si no hay id. Si el id ya está ligado a fechas fuera del batch, clona en lugar de mutar.
     */
    private TourScheduleConfig resolveConfigForBulk(
            TourScheduleConfigDto configDto,
            Integer tourId,
            Integer currentScheduleConfigId,
            Authentication connectedUser,
            BulkConfigState bulkConfigState) {
        if (configDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "config is required.");
        }

        Integer normalizedConfigId = PayloadIdUtils.normalizeId(configDto.getId());
        final Integer targetConfigId = normalizedConfigId != null ? normalizedConfigId : currentScheduleConfigId;

        if (targetConfigId == null) {
            Integer createdId = bulkConfigState.configsCreatedByTour.get(tourId);
            if (createdId != null) {
                return tourScheduleConfigRepository.findByIdWithSlots(createdId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Config not found after batch create: " + createdId));
            }
            TourScheduleConfig created = createConfigFromDto(configDto, tourId, connectedUser);
            bulkConfigState.configsCreatedByTour.put(tourId, created.getId());
            return created;
        }

        Integer effectiveConfigId = bulkConfigState.configRemapInBatch.getOrDefault(targetConfigId, targetConfigId);
        TourScheduleConfig config = tourScheduleConfigRepository.findByIdWithSlots(effectiveConfigId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour configuration with ID " + effectiveConfigId + " not found."));
        if (!Objects.equals(config.getTourId(), tourId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Config " + targetConfigId + " does not belong to tour " + tourId);
        }

        if (bulkConfigState.configRemapInBatch.containsKey(targetConfigId)) {
            return config;
        }

        if (hasConfigPayload(configDto) && !bulkConfigState.configsHandledInBatch.contains(targetConfigId)) {
            bulkConfigState.configsHandledInBatch.add(targetConfigId);
            if (isConfigUsedOutsideBatchDates(targetConfigId, bulkConfigState.batchDates)) {
                TourScheduleConfig cloned = createConfigFromDto(configDto, tourId, connectedUser);
                bulkConfigState.configRemapInBatch.put(targetConfigId, cloned.getId());
                return cloned;
            }
            TourScheduleConfigCreationRequest request = mapDtoToCreationRequest(configDto, tourId);
            TourScheduleConfigResponse updated = updateTourScheduleConfig(targetConfigId, request, connectedUser);
            return tourScheduleConfigRepository.findByIdWithSlots(updated.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Config not found after update: " + updated.getId()));
        }

        return config;
    }

    private boolean isConfigUsedOutsideBatchDates(Integer configId, Set<LocalDate> batchDates) {
        if (batchDates == null || batchDates.isEmpty()) {
            return false;
        }
        return tourScheduleRepository.findByConfigId(configId).stream()
                .anyMatch(schedule -> !batchDates.contains(schedule.getScheduleDate()));
    }

    private boolean hasConfigPayload(TourScheduleConfigDto configDto) {
        return configDto.getLabel() != null
                || (configDto.getDaysOfWeek() != null && !configDto.getDaysOfWeek().isEmpty())
                || (configDto.getSlots() != null && !configDto.getSlots().isEmpty());
    }

    /**
     * Crea un nuevo TourScheduleConfig a partir de un DTO
     */
    private TourScheduleConfig createConfigFromDto(
            TourScheduleConfigDto configDto,
            Integer tourId,
            Authentication connectedUser) {

        TourScheduleConfigCreationRequest request = mapDtoToCreationRequest(configDto, tourId);

        TourScheduleConfigResponse response = createTourScheduleConfig(request, connectedUser);

        return tourScheduleConfigRepository
                .findById(response.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Config not found after creation: " + response.getId()));
    }

    /**
     * Crea un nuevo TourSchedule a partir de un DTO y config
     */
    private TourSchedule createScheduleFromDto(
            TourScheduleRequest dto,
            TourScheduleConfig config) {

        TourSchedule schedule = new TourSchedule();
        schedule.setTourId(dto.getTourId());
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStatus(dto.getStatus());
        schedule.setConfig(config); // Usar setConfig() en lugar de setConfigId()

        return schedule;
    }

    /**
     * Actualiza las propiedades de un TourSchedule existente
     */
    private void updateScheduleProperties(
            TourSchedule schedule,
            TourScheduleRequest dto) {

        schedule.setStatus(dto.getStatus());
    }

    /**
     * Mapea un TourScheduleConfigDto a TourScheduleConfigCreationRequest
     */
    private TourScheduleConfigCreationRequest mapDtoToCreationRequest(
            TourScheduleConfigDto dto,
            Integer tourId) {

        TourScheduleConfigCreationRequest request = new TourScheduleConfigCreationRequest();

        request.setId(dto.getId());
        request.setTourId(tourId); // Usar el tourId del request padre, no del DTO
        request.setProviderId(dto.getProviderId());
        request.setLabel(dto.getLabel());
        request.setDaysOfWeek(dto.getDaysOfWeek());
        request.setSlots(dto.getSlots());
        request.setIsTemplate(false);

        return request;
    }

    private TourScheduleBulkResponse buildBulkResponse(
            TourScheduleRequest dto,
            TourSchedule savedSchedule,
            TourScheduleConfig config,
            Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        List<Role> roleList = user.getRoles();

        TourScheduleBulkResponse response = new TourScheduleBulkResponse();
        response.setScheduleId(savedSchedule.getId());
        response.setTourId(dto.getTourId());
        response.setScheduleDate(dto.getScheduleDate());

        TourScheduleConfig refreshed = tourScheduleConfigRepository.findByIdWithSlots(config.getId())
                .orElse(config);
        response.setConfig(mapToTourScheduleConfigResponse(refreshed, roleList));
        return response;
    }
}

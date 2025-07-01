package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourSchedule;
import com.tourya.api.models.TourScheduleConfig;
import com.tourya.api.models.TourScheduleConfigPrice;
import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.models.User;
import com.tourya.api.models.request.TourScheduleConfigCreationRequest;
import com.tourya.api.models.request.TourScheduleConfigPriceDto;
import com.tourya.api.models.request.TourScheduleConfigSlotDto;
import com.tourya.api.models.responses.TourScheduleConfigResponseDto;
import com.tourya.api.models.responses.TourSchedulePriceResponseDto;
import com.tourya.api.models.responses.TourScheduleResponseDto;
import com.tourya.api.models.responses.TourScheduleSlotResponseDto;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.repository.TourScheduleConfigRepository;
import com.tourya.api.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourScheduleService {
    private final TourScheduleRepository tourScheduleRepository;
    private final TourScheduleConfigRepository tourScheduleConfigRepository;
    private final TourRepository tourRepository;
    private final ProviderService providerService;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    private Tour getTour(Integer tourId, Integer providerId){
        Tour tour =  tourRepository.findTourByIdAndProviderId(tourId, providerId);
        if(tour != null){
            return tour;
        }else{
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }

    @Transactional // Asegura que todas las operaciones se realicen como una única transacción
    public TourScheduleConfigResponseDto createTourScheduleConfigAndGenerateSchedules(
            TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)) {
            Provider provider = providerService.findByUserAndStatusActive(user);
            // 1. Validar que el tourId existe
            Tour tour = getTour(request.getTourId(), provider.getId());

            /*Tour tour = tourRepository.findById(request.getTourId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Tour con ID " + request.getTourId() + " no encontrado."));*/

            // 2. Crear y guardar TourScheduleConfig
            TourScheduleConfig config = new TourScheduleConfig();
            config.setTourId(request.getTourId());
            config.setLabel(request.getLabel());
            config.setStartDate(request.getStartDate());
            config.setEndDate(request.getEndDate());
            // Convertir los Strings de daysOfWeek a un Set de DayOfWeek para una búsqueda eficiente
            // Manejar posibles errores si el string no es un nombre de día de la semana válido
            Set<DayOfWeek> validDaysOfWeek = request.getDaysOfWeek().stream()
                    .map(String::toUpperCase) // Asegurar mayúsculas para el enum
                    .map(dayStr -> {
                        try {
                            return DayOfWeek.valueOf(dayStr);
                        } catch (IllegalArgumentException e) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Día de la semana inválido: " + dayStr + ". Debe ser uno de los valores de java.time.DayOfWeek (ej. MONDAY).");
                        }
                    })
                    .collect(Collectors.toSet());
            // Volver a guardar como List<String> para la entidad (debido al mapeo TypeDef)
            config.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));

            config.setIsUnlimitedCapacity(request.getIsUnlimitedCapacity() != null ? request.getIsUnlimitedCapacity() : false);
            //config.setCreatedBy(request.getCreatedBy()); // Idealmente, esto vendría del contexto de seguridad

            TourScheduleConfig savedConfig = tourScheduleConfigRepository.save(config);

            // 3. Crear y guardar TourScheduleConfigSlot y TourScheduleConfigPrice
            // Las relaciones OneToMany en la entidad config se manejarán por JPA con CascadeType.ALL
            // Por eso, se añaden los slots directamente a la lista de la configuración
            Set<TourScheduleConfigSlot> newSlots = new HashSet<>();
            if (request.getSlots() != null) {
                for (TourScheduleConfigSlotDto slotDto : request.getSlots()) {
                    TourScheduleConfigSlot slot = new TourScheduleConfigSlot();
                    slot.setConfig(savedConfig); // Asociar al objeto config completo
                    slot.setStartTime(slotDto.getStartTime());
                    slot.setEndTime(slotDto.getEndTime());
                    slot.setMinCapacity(slotDto.getMinCapacity());
                    slot.setMaxCapacity(slotDto.getMaxCapacity());
                    //slot.setCreatedBy(request.getCreatedBy());

                    // Manejo de precios para el slot
                    if (slotDto.getPrices() != null) {
                        Set<TourScheduleConfigPrice> newPrices = new HashSet<>();
                        for (TourScheduleConfigPriceDto priceDto : slotDto.getPrices()) {
                            TourScheduleConfigPrice price = new TourScheduleConfigPrice();
                            price.setSlot(slot); // Asociar al slot
                            price.setAgeType(priceDto.getAgeType());
                            price.setMinAge(priceDto.getMinAge());
                            price.setMaxAge(priceDto.getMaxAge());
                            price.setPrice(priceDto.getPrice());
                            //price.setCreatedBy(request.getCreatedBy());
                            newPrices.add(price);
                        }
                        slot.setPrices(newPrices);
                    }
                    newSlots.add(slot);
                }
            }
            savedConfig.setSlots(newSlots); // Asignar los nuevos slots a la configuración
            tourScheduleConfigRepository.save(savedConfig); // Guardar de nuevo para que los slots se persistan en cascada

            // 4. Generar y guardar TourSchedule para cada día aplicable
            generateAndSaveTourSchedules(savedConfig, validDaysOfWeek);

            // CAMBIO: Llamar al método de consulta para obtener el DTO completo para la respuesta
            return getTourScheduleConfigDetails(savedConfig.getId());
        }else {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
   }

    @Transactional
    public TourScheduleConfigResponseDto updateTourScheduleConfigAndGenerateSchedules(
            Integer configId, TourScheduleConfigCreationRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)) {
            Provider provider = providerService.findByUserAndStatusActive(user);

            // 1. Obtener la configuración existente
            // Usar findByIdWithSlots para asegurar que los slots se carguen EAGERLY para la manipulación
            TourScheduleConfig existingConfig = tourScheduleConfigRepository.findByIdWithSlots(configId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Configuración de Tour con ID " + configId + " no encontrada."));

            // Validar que el tourId existe
            Tour tour = getTour(request.getTourId(), provider.getId());
            /*Tour tour = tourRepository.findById(request.getTourId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Tour con ID " + request.getTourId() + " no encontrado."));*/

            // 2. Actualizar las propiedades de la configuración principal
            existingConfig.setTourId(request.getTourId());
            existingConfig.setLabel(request.getLabel());
            existingConfig.setStartDate(request.getStartDate());
            existingConfig.setEndDate(request.getEndDate());
            // Actualizar last_modified_by
            //existingConfig.setLastModifiedBy(request.getCreatedBy()); // Usamos createdBy como lastModifiedBy para simplicidad

            // Validar y actualizar daysOfWeek
            Set<DayOfWeek> newValidDaysOfWeek = request.getDaysOfWeek().stream()
                    .map(String::toUpperCase)
                    .map(dayStr -> {
                        try {
                            return DayOfWeek.valueOf(dayStr);
                        } catch (IllegalArgumentException e) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Día de la semana inválido: " + dayStr + ". Debe ser uno de los valores de java.time.DayOfWeek (ej. MONDAY).");
                        }
                    })
                    .collect(Collectors.toSet());
            existingConfig.setDaysOfWeek(new ArrayList<>(request.getDaysOfWeek()));

            existingConfig.setIsUnlimitedCapacity(request.getIsUnlimitedCapacity() != null ? request.getIsUnlimitedCapacity() : false);

            // --- REFACTORIZACIÓN DE LA LÓGICA PARA GESTIONAR SLOTS Y PRECIOS ---

            // Paso 1: Crear un mapa de los slots existentes POR ID para una búsqueda eficiente
            // Se usa Function.identity() para mapear el slot a sí mismo, asumiendo que ya tienen IDs válidos
            Map<Integer, TourScheduleConfigSlot> existingSlotsMap = existingConfig.getSlots().stream()
                    .filter(s -> s.getId() != null) // **Asegurar que solo slots con ID válido sean mapeados**
                    .collect(Collectors.toMap(TourScheduleConfigSlot::getId, Function.identity()));

            // Paso 2: Preparar una nueva colección para los slots que deben persistir (actualizados o nuevos)
            Set<TourScheduleConfigSlot> newOrUpdatedSlots = new HashSet<>();

            if (request.getSlots() != null) {
                for (TourScheduleConfigSlotDto slotDto : request.getSlots()) {
                    TourScheduleConfigSlot currentSlot;

                    if (slotDto.getId() != null && existingSlotsMap.containsKey(slotDto.getId())) {
                        // Es un slot existente: obtener la instancia gestionada
                        currentSlot = existingSlotsMap.get(slotDto.getId());
                        //currentSlot.setLastModifiedBy(request.getCreatedBy());
                    } else {
                        // Es un slot nuevo: crear una nueva instancia
                        currentSlot = new TourScheduleConfigSlot();
                        //currentSlot.setCreatedBy(request.getCreatedBy());
                        currentSlot.setConfig(existingConfig); // Asociar al padre
                    }

                    // Actualizar propiedades del slot
                    currentSlot.setStartTime(slotDto.getStartTime());
                    currentSlot.setEndTime(slotDto.getEndTime());
                    currentSlot.setMinCapacity(slotDto.getMinCapacity());
                    currentSlot.setMaxCapacity(slotDto.getMaxCapacity());

                    // Manejar los precios de este slot
                    updateSlotPrices(currentSlot, slotDto.getPrices(), request.getCreatedBy());

                    newOrUpdatedSlots.add(currentSlot); // Añadir a la colección que persistirá
                }
            }

            // Paso 3: Limpiar la colección existente. Esto marca los slots que no están en newOrUpdatedSlots como huérfanos.
            existingConfig.getSlots().clear();
            // Paso 4: Añadir todos los slots (actualizados y nuevos) a la colección gestionada.
            existingConfig.getSlots().addAll(newOrUpdatedSlots);

            // --- FIN DE LA REFACTORIZACIÓN PARA GESTIONAR SLOTS Y PRECIOS ---


            TourScheduleConfig savedConfig = tourScheduleConfigRepository.save(existingConfig); // Persistir los cambios en cascada
            generateAndSaveTourSchedules(savedConfig, newValidDaysOfWeek);

            // CAMBIO: Llamar al método de consulta para obtener el DTO completo para la respuesta
            return getTourScheduleConfigDetails(savedConfig.getId());
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

    }

    /**
     * Helper method to manage the prices within a TourScheduleConfigSlot.
     * This method correctly handles additions, updates, and deletions of prices.
     *
     * @param slot The parent TourScheduleConfigSlot whose prices are being managed.
     * @param incomingPriceDtos The list of TourScheduleConfigPriceDto from the request.
     * @param userId The ID of the user performing the update/creation.
     */
    private void updateSlotPrices(TourScheduleConfigSlot slot, List<TourScheduleConfigPriceDto> incomingPriceDtos, Long userId) {
        // Map de precios DTO por ID para búsqueda rápida de los que vienen en el request
        Map<Integer, TourScheduleConfigPriceDto> incomingPriceDtosById = incomingPriceDtos != null ?
                incomingPriceDtos.stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(TourScheduleConfigPriceDto::getId, Function.identity()))
                : new java.util.HashMap<>();

        // Recorrer los precios existentes para identificar cuales se eliminan
        List<TourScheduleConfigPrice> pricesToDelete = slot.getPrices().stream()
                .filter(existingPrice -> !incomingPriceDtosById.containsKey(existingPrice.getId()))
                .collect(Collectors.toList());

        // Eliminar los precios que ya no están en el request (orphanRemoval=true los borrará de DB)
        slot.getPrices().removeAll(pricesToDelete);

        // Iterar sobre los precios del DTO para añadir/actualizar
        if (incomingPriceDtos != null) {
            for (TourScheduleConfigPriceDto priceDto : incomingPriceDtos) {
                TourScheduleConfigPrice currentPrice;

                if (priceDto.getId() != null) {
                    // Intenta encontrar el precio existente cargado por el fetch join del slot
                    currentPrice = slot.getPrices().stream()
                            .filter(p -> p.getId().equals(priceDto.getId()))
                            .findFirst()
                            .orElse(null);
                } else {
                    currentPrice = null;
                }

                if (currentPrice == null) {
                    // Es un precio nuevo
                    currentPrice = new TourScheduleConfigPrice();
                    //currentPrice.setCreatedBy(userId);
                    currentPrice.setSlot(slot); // Asociar al padre
                    slot.getPrices().add(currentPrice); // Añadir a la colección gestionada
                } else {
                    // Precio existente, actualizar lastModifiedBy
                    //currentPrice.setLastModifiedBy(userId);
                }

                // Actualizar propiedades del precio
                currentPrice.setAgeType(priceDto.getAgeType());
                currentPrice.setMinAge(priceDto.getMinAge());
                currentPrice.setMaxAge(priceDto.getMaxAge());
                currentPrice.setPrice(priceDto.getPrice());
            }
        }
    }

    /**
     * Helper method to generate and save TourSchedule entries based on a given configuration.
     * This method is designed to be called by both creation and update flows.
     * It handles preserving reserved capacity and status for matching existing schedules.
     * @param config The TourScheduleConfig to base schedules on.
     * @param validDaysOfWeek The set of DayOfWeek enums for valid days.
     */
    private void generateAndSaveTourSchedules(TourScheduleConfig config, Set<DayOfWeek> validDaysOfWeek) {
        // Almacenar reserved_capacity y status de los schedules existentes antes de eliminarlos
        Map<String, TourSchedule> existingSchedulesMap = tourScheduleRepository.findByConfigId(config.getId())
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getScheduleDate().toString() + "_" + s.getStartTime().toString() + "_" + s.getEndTime().toString(),
                        Function.identity()
                ));

        // Eliminar todos los TourSchedule existentes para esta configuración
        tourScheduleRepository.deleteByConfigId(config.getId());

        List<TourSchedule> generatedSchedules = new ArrayList<>();
        LocalDate currentDate = config.getStartDate();
        while (!currentDate.isAfter(config.getEndDate())) {
            // Check if current day is one of the selected days of week
            if (validDaysOfWeek.contains(currentDate.getDayOfWeek())) {
                for (TourScheduleConfigSlot slot : config.getSlots()) { // Usa los slots asociados a la config
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

                    // Intentar restaurar reserved_capacity y status si existían
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
                    schedule.setConfigId(config.getId()); // <-- AÑADIDO ESTA LÍNEA
                    schedule.setCreatedBy(config.getCreatedBy()); // Se mantiene el creador original
                    schedule.setLastModifiedBy(config.getLastModifiedBy()); // Se actualiza el modificador

                    generatedSchedules.add(schedule);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        tourScheduleRepository.saveAll(generatedSchedules); // Guardar todos los horarios generados
    }

    /**
     * Recupera los detalles completos de una configuración de tour, incluyendo sus slots, precios y horarios generados.
     *
     * @param configId El ID de la configuración de tour.
     * @return Un DTO con todos los detalles de la configuración.
     * @throws ResponseStatusException si la configuración no se encuentra.
     */
    @Transactional(readOnly = true) // Solo lectura, no modifica la base de datos
    public TourScheduleConfigResponseDto getTourScheduleConfigDetails(Integer configId) {
        // 1. Obtener la configuración con slots y precios
        TourScheduleConfig config = tourScheduleConfigRepository.findByIdWithSlots(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Configuración de Tour con ID " + configId + " no encontrada."));

        // 2. Obtener los horarios de tour generados asociados a esta configuración
        List<TourSchedule> schedules = tourScheduleRepository.findByConfigId(configId);

        // 3. Mapear entidades a DTOs de respuesta
        TourScheduleConfigResponseDto responseDto = new TourScheduleConfigResponseDto();
        responseDto.setId(config.getId());
        responseDto.setTourId(config.getTourId());
        responseDto.setLabel(config.getLabel());
        responseDto.setStartDate(config.getStartDate());
        responseDto.setEndDate(config.getEndDate());
        responseDto.setDaysOfWeek(config.getDaysOfWeek());
        responseDto.setIsUnlimitedCapacity(config.getIsUnlimitedCapacity());
        //responseDto.setCreatedBy(config.getCreatedBy());
        //responseDto.setLastModifiedBy(config.getLastModifiedBy());
        responseDto.setCreatedDate(config.getCreatedDate());
        responseDto.setLastModifiedDate(config.getLastModifiedDate());

        // Mapear slots y sus precios
        Set<TourScheduleSlotResponseDto> slotDtos = config.getSlots().stream()
                .map(slot -> {
                    TourScheduleSlotResponseDto slotDto = new TourScheduleSlotResponseDto();
                    slotDto.setId(slot.getId());
                    slotDto.setStartTime(slot.getStartTime());
                    slotDto.setEndTime(slot.getEndTime());
                    slotDto.setMinCapacity(slot.getMinCapacity());
                    slotDto.setMaxCapacity(slot.getMaxCapacity());

                    Set<TourSchedulePriceResponseDto> priceDtos = slot.getPrices().stream()
                            .map(price -> {
                                TourSchedulePriceResponseDto priceDto = new TourSchedulePriceResponseDto();
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

        // Mapear horarios generados
        List<TourScheduleResponseDto> scheduleDtos = schedules.stream()
                .map(schedule -> {
                    TourScheduleResponseDto scheduleDto = new TourScheduleResponseDto();
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
}

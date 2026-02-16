package com.tourya.api.services;

import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.models.AgeRangeConfig;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.repository.AgeRangeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar la configuración de rangos de edad.
 * Proporciona métodos para obtener configuraciones por tipo de edad
 * y cachea los resultados para mejorar el rendimiento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgeRangeConfigService {

    private final AgeRangeConfigRepository repository;

    /**
     * Obtiene la configuración de rango de edad para un tipo específico.
     *
     * @param ageType el tipo de edad (ADULT, CHILD, INFANT)
     * @return la configuración de rango de edad
     * @throws ResourceNotFoundException si no se encuentra la configuración
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "ageRangeConfig", key = "#ageType")
    public AgeRangeConfig getByAgeType(AgePriceType ageType) {
        log.debug("Fetching age range config for type: {}", ageType);
        return repository.findByAgeType(ageType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Age range configuration not found for type: " + ageType));
    }

    /**
     * Obtiene todas las configuraciones activas como un mapa.
     * Útil para mapear rápidamente tipos de edad a sus configuraciones.
     *
     * @return mapa de AgePriceType a AgeRangeConfig
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "ageRangeConfigMap")
    public Map<AgePriceType, AgeRangeConfig> getAllAsMap() {
        log.debug("Fetching all active age range configs as map");
        List<AgeRangeConfig> configs = repository.findByIsActiveTrue();

        if (configs.isEmpty()) {
            log.warn("No active age range configurations found in database");
        }

        return configs.stream()
                .collect(Collectors.toMap(
                        AgeRangeConfig::getAgeType,
                        Function.identity()));
    }

    /**
     * Obtiene todas las configuraciones activas.
     *
     * @return lista de configuraciones activas
     */
    @Transactional(readOnly = true)
    public List<AgeRangeConfig> getAllActive() {
        log.debug("Fetching all active age range configs");
        return repository.findByIsActiveTrue();
    }

    /**
     * Verifica si existe una configuración para un tipo de edad.
     *
     * @param ageType el tipo de edad
     * @return true si existe, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean existsByAgeType(AgePriceType ageType) {
        return repository.existsByAgeType(ageType);
    }
}

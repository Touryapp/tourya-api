package com.tourya.api.services;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.AppConfig;
import com.tourya.api.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Servicio para gestionar configuraciones del sistema.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;

    /**
     * Obtiene el valor de una configuración por su clave
     * 
     * @param configKey Clave de la configuración
     * @return Valor de la configuración (JSON)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConfigValue(ConfigKeyEnum configKey) {
        log.info("Getting config value for key: {}", configKey.getValue());
        
        AppConfig config = appConfigRepository.findByConfigKey(configKey.getValue())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Config not found with key: " + configKey.getValue()));
        
        return config.getConfigValue();
    }

    /**
     * Obtiene la configuración completa por su clave
     * 
     * @param configKey Clave de la configuración
     * @return Entidad AppConfig
     */
    @Transactional(readOnly = true)
    public AppConfig getConfig(ConfigKeyEnum configKey) {
        log.info("Getting config for key: {}", configKey.getValue());
        
        return appConfigRepository.findByConfigKey(configKey.getValue())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Config not found with key: " + configKey.getValue()));
    }
}


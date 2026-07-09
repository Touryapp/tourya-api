package com.tourya.api.services;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.AppConfig;
import com.tourya.api.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Lee un valor entero de app_config con fallback silencioso al valor default.
     * Usa el JSON pattern `{"value": N}`. Si la key no existe o el JSON no tiene "value"
     * o no es numerico, retorna el fallback (comportamiento equivalente al hardcoded).
     * NO lanza excepcion: uso desde servicios criticos que no deben fallar por config.
     */
    @Transactional(readOnly = true)
    public int getInt(ConfigKeyEnum configKey, int fallback) {
        Optional<AppConfig> found = appConfigRepository.findByConfigKey(configKey.getValue());
        if (found.isEmpty()) {
            return fallback;
        }
        Map<String, Object> raw = found.get().getConfigValue();
        if (raw == null) {
            return fallback;
        }
        Object v = raw.get("value");
        if (v instanceof Number number) {
            return number.intValue();
        }
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                log.warn("app_config key={} tiene value no numerico ({}), usando fallback={}", configKey.getValue(), s, fallback);
                return fallback;
            }
        }
        log.warn("app_config key={} tiene formato inesperado, usando fallback={}", configKey.getValue(), fallback);
        return fallback;
    }

    /**
     * Upsert de configuracion por clave. Si existe, actualiza value y description; si no, crea.
     * La auditoria (created_by / last_modified_by) se maneja automaticamente via JPA Auditing.
     */
    @Transactional
    public AppConfig upsertConfig(ConfigKeyEnum configKey, Map<String, Object> value, String description) {
        AppConfig entity = appConfigRepository.findByConfigKey(configKey.getValue())
                .orElseGet(() -> AppConfig.builder()
                        .configKey(configKey.getValue())
                        .configValue(new HashMap<>())
                        .build());
        entity.setConfigValue(value);
        if (description != null && !description.isBlank()) {
            entity.setDescription(description);
        }
        return appConfigRepository.save(entity);
    }
}


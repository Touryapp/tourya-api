package com.tourya.api.repository;

import com.tourya.api.models.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad AppConfig.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {

    /**
     * Busca una configuración por su clave
     */
    Optional<AppConfig> findByConfigKey(String configKey);
}


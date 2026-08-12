package com.tourya.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * TC-020 #235 bug (c): the default DateTimeProvider used by Spring Data JPA auditing
 * relies on the JVM zone. Cloud Run runs in UTC, so @CreatedDate / @LastModifiedDate
 * were persisted in UTC. Turistas veian fechas +5h.
 *
 * This bean forces every LocalDateTime stamped by @CreatedDate / @LastModifiedDate
 * to America/Bogota, matching every explicit LocalDateTime.now(BOGOTA) already used
 * across services and jobs.
 */
@Configuration
public class JpaAuditingConfig {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Bean(name = "bogotaDateTimeProvider")
    public DateTimeProvider bogotaDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(BOGOTA));
    }
}

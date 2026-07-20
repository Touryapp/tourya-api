package com.tourya.api.services.maritime.events;

import com.tourya.api.constans.enums.MaritimeFlagEnum;

import java.time.LocalDate;

/**
 * BE-23: se publica desde {@code MaritimActivityReportService.create()} cuando el
 * reporte tiene {@link MaritimeFlagEnum#RED}. Un listener con
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} lo consume para
 * cancelar reservas afectadas + crear creditos + notificar.
 *
 * <p>Snapshot inmutable de los campos necesarios para la query — no lleva
 * la entidad viva para evitar problemas de sesion JPA cerrada en el listener
 * async.</p>
 */
public record MaritimeAlertCreatedEvent(
        Long reportId,
        String subcategoryCode,
        Integer countryId,
        Integer stateId,
        Integer cityId,
        LocalDate startDate,
        LocalDate endDate,
        MaritimeFlagEnum flag
) {}

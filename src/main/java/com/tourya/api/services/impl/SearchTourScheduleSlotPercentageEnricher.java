package com.tourya.api.services.impl;

import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.services.TourScheduleOverrideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aplica {@code slotPorcentajeTourya} y precios efectivos por schedule (override por día).
 * Sin override por día se usa 0% y precio proveedor, nunca el slot compartido del config.
 */
@Component
@RequiredArgsConstructor
public class SearchTourScheduleSlotPercentageEnricher {

    private final TourScheduleOverrideService tourScheduleOverrideService;

    public void enrich(List<SearchTourScheduleFullResponse> rows, boolean showSlotPercentage) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        tourScheduleOverrideService.enrichSearchRows(rows, showSlotPercentage);
    }
}

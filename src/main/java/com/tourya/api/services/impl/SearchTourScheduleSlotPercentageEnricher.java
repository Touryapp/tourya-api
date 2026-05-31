package com.tourya.api.services.impl;

import com.tourya.api._utils.TouryaPriceCalculator;
import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.TourScheduleConfigSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enriquece resultados de búsqueda con {@code slotPorcentajeTourya} (solo backoffice).
 */
@Component
@RequiredArgsConstructor
public class SearchTourScheduleSlotPercentageEnricher {

    private final TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;

    public void enrich(List<SearchTourScheduleFullResponse> rows, boolean showSlotPercentage) {
        if (!showSlotPercentage || rows == null || rows.isEmpty()) {
            return;
        }
        Set<Integer> slotIds = collectSlotIds(rows);
        if (slotIds.isEmpty()) {
            return;
        }
        Map<Integer, BigDecimal> slotPercentById = loadSlotPercentPointsBySlotIds(slotIds);
        applyPercentToRows(rows, slotPercentById);
    }

    private Set<Integer> collectSlotIds(List<SearchTourScheduleFullResponse> rows) {
        Set<Integer> ids = new HashSet<>();
        for (SearchTourScheduleFullResponse row : rows) {
            if (row == null || row.getSchedules() == null) {
                continue;
            }
            for (SearchTourScheduleFullResponse.TourScheduleResponse sch : row.getSchedules()) {
                if (sch == null || sch.getConfig() == null || sch.getConfig().getSlots() == null) {
                    continue;
                }
                for (SearchTourScheduleFullResponse.TourScheduleSlotResponse slot : sch.getConfig().getSlots()) {
                    if (slot != null && slot.getSlotId() != null) {
                        ids.add(slot.getSlotId());
                    }
                }
            }
        }
        return ids;
    }

    private Map<Integer, BigDecimal> loadSlotPercentPointsBySlotIds(Set<Integer> slotIds) {
        Map<Integer, BigDecimal> out = new HashMap<>();
        for (TourScheduleConfigSlot slot : tourScheduleConfigSlotRepository.findAllById(slotIds)) {
            out.put(slot.getId(), TouryaPriceCalculator.toApiPercentPoints(slot.getSlotPorcentajeTourya()));
        }
        return out;
    }

    private void applyPercentToRows(List<SearchTourScheduleFullResponse> rows, Map<Integer, BigDecimal> slotPercentById) {
        for (SearchTourScheduleFullResponse row : rows) {
            if (row == null || row.getSchedules() == null) {
                continue;
            }
            for (SearchTourScheduleFullResponse.TourScheduleResponse sch : row.getSchedules()) {
                if (sch == null || sch.getConfig() == null || sch.getConfig().getSlots() == null) {
                    continue;
                }
                for (SearchTourScheduleFullResponse.TourScheduleSlotResponse slot : sch.getConfig().getSlots()) {
                    if (slot != null && slot.getSlotId() != null) {
                        slot.setSlotPorcentajeTourya(slotPercentById.get(slot.getSlotId()));
                    }
                }
            }
        }
    }
}

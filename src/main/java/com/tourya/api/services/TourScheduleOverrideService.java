package com.tourya.api.services;

import com.tourya.api._utils.TouryaPriceCalculator;
import com.tourya.api.models.TourScheduleConfigPrice;
import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.models.TourSchedulePriceOverride;
import com.tourya.api.models.TourScheduleSlotOverride;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.models.responses.TourScheduleConfigResponse;
import com.tourya.api.models.responses.TourSchedulePriceResponse;
import com.tourya.api.models.responses.TourScheduleSlotResponse;
import com.tourya.api.repository.TourSchedulePriceOverrideRepository;
import com.tourya.api.repository.TourScheduleSlotOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Porcentaje y precio efectivos por instancia de {@code tour_schedule} (día).
 */
@Service
@RequiredArgsConstructor
public class TourScheduleOverrideService {

    private final TourScheduleSlotOverrideRepository slotOverrideRepository;
    private final TourSchedulePriceOverrideRepository priceOverrideRepository;

    @Transactional
    public void upsertSlotOverride(Integer scheduleId, Integer slotId, BigDecimal fraction) {
        TourScheduleSlotOverride row = slotOverrideRepository.findByScheduleIdAndSlotId(scheduleId, slotId)
                .orElseGet(() -> TourScheduleSlotOverride.builder()
                        .scheduleId(scheduleId)
                        .slotId(slotId)
                        .build());
        row.setSlotPorcentajeTourya(fraction);
        slotOverrideRepository.save(row);
    }

    @Transactional
    public void upsertPriceOverride(Integer scheduleId, Integer priceId, BigDecimal price) {
        TourSchedulePriceOverride row = priceOverrideRepository.findByScheduleIdAndPriceId(scheduleId, priceId)
                .orElseGet(() -> TourSchedulePriceOverride.builder()
                        .scheduleId(scheduleId)
                        .priceId(priceId)
                        .build());
        row.setPrice(price);
        priceOverrideRepository.save(row);
    }

    @Transactional
    public int applyPercentageToScheduleSlots(Integer scheduleId, Collection<TourScheduleConfigSlot> slots,
            BigDecimal fraction) {
        int pricesUpdated = 0;
        for (TourScheduleConfigSlot slot : slots) {
            upsertSlotOverride(scheduleId, slot.getId(), fraction);
            if (slot.getPrices() == null) {
                continue;
            }
            for (TourScheduleConfigPrice price : slot.getPrices()) {
                if (price.getProviderPrice() == null) {
                    continue;
                }
                BigDecimal salePrice = TouryaPriceCalculator.calculateSalePrice(price.getProviderPrice(), fraction);
                upsertPriceOverride(scheduleId, price.getId(), salePrice);
                pricesUpdated++;
            }
        }
        return pricesUpdated;
    }

    @Transactional
    public int applyPercentageToScheduleSlot(Integer scheduleId, TourScheduleConfigSlot slot, BigDecimal fraction) {
        upsertSlotOverride(scheduleId, slot.getId(), fraction);
        int pricesUpdated = 0;
        if (slot.getPrices() == null) {
            return pricesUpdated;
        }
        for (TourScheduleConfigPrice price : slot.getPrices()) {
            if (price.getProviderPrice() == null) {
                continue;
            }
            BigDecimal salePrice = TouryaPriceCalculator.calculateSalePrice(price.getProviderPrice(), fraction);
            upsertPriceOverride(scheduleId, price.getId(), salePrice);
            pricesUpdated++;
        }
        return pricesUpdated;
    }

    public BigDecimal resolveSalePrice(Integer scheduleId, Integer priceId, BigDecimal defaultPrice) {
        if (scheduleId == null || priceId == null) {
            return defaultPrice;
        }
        return priceOverrideRepository.findByScheduleIdAndPriceId(scheduleId, priceId)
                .map(TourSchedulePriceOverride::getPrice)
                .orElse(defaultPrice);
    }

    public void applyToConfigResponse(Integer scheduleId, TourScheduleConfigResponse config,
            boolean showSlotPercentage) {
        if (scheduleId == null || config == null || config.getSlots() == null) {
            return;
        }
        OverrideBatchData batch = loadOverrideBatch(List.of(scheduleId));
        applyToConfigResponse(scheduleId, config, batch, showSlotPercentage);
    }

    public OverrideBatchData loadOverrideBatch(Collection<Integer> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return new OverrideBatchData(Map.of(), Map.of());
        }
        return new OverrideBatchData(
                loadSlotFractionMaps(scheduleIds),
                loadPriceMaps(scheduleIds));
    }

    public void applyToConfigResponse(Integer scheduleId, TourScheduleConfigResponse config,
            OverrideBatchData batch, boolean showSlotPercentage) {
        if (scheduleId == null || config == null || config.getSlots() == null || batch == null) {
            return;
        }
        Map<Integer, BigDecimal> slotFractionById = batch.slotFractionByScheduleId()
                .getOrDefault(scheduleId, Map.of());
        Map<Integer, BigDecimal> priceById = batch.priceByScheduleId()
                .getOrDefault(scheduleId, Map.of());
        for (TourScheduleSlotResponse slot : config.getSlots()) {
            applyToSlotResponse(slot, slotFractionById, priceById, showSlotPercentage);
        }
    }

    public record OverrideBatchData(
            Map<Integer, Map<Integer, BigDecimal>> slotFractionByScheduleId,
            Map<Integer, Map<Integer, BigDecimal>> priceByScheduleId) {
    }

    public void enrichSearchRows(List<SearchTourScheduleFullResponse> rows, boolean showSlotPercentage) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Integer> scheduleIds = rows.stream()
                .filter(r -> r.getSchedules() != null)
                .flatMap(r -> r.getSchedules().stream())
                .map(SearchTourScheduleFullResponse.TourScheduleResponse::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (scheduleIds.isEmpty()) {
            return;
        }
        Map<Integer, Map<Integer, BigDecimal>> slotPctBySchedule = loadSlotFractionMaps(scheduleIds);

        for (SearchTourScheduleFullResponse row : rows) {
            if (row.getSchedules() == null) {
                continue;
            }
            for (SearchTourScheduleFullResponse.TourScheduleResponse sch : row.getSchedules()) {
                applySearchScheduleOverrides(sch, slotPctBySchedule, showSlotPercentage);
            }
        }
    }

    private void applySearchScheduleOverrides(
            SearchTourScheduleFullResponse.TourScheduleResponse sch,
            Map<Integer, Map<Integer, BigDecimal>> slotPctBySchedule,
            boolean showSlotPercentage) {
        if (sch.getId() == null || sch.getConfig() == null || sch.getConfig().getSlots() == null) {
            return;
        }
        Map<Integer, BigDecimal> slotPct = slotPctBySchedule.getOrDefault(sch.getId(), Map.of());
        for (SearchTourScheduleFullResponse.TourScheduleSlotResponse slot : sch.getConfig().getSlots()) {
            Integer slotId = slot.getSlotId();
            if (slotId == null || !slotPct.containsKey(slotId)) {
                continue;
            }
            BigDecimal fraction = slotPct.get(slotId);
            if (showSlotPercentage) {
                slot.setSlotPorcentajeTourya(TouryaPriceCalculator.toApiPercentPoints(fraction));
            }
            if (slot.getPrices() == null) {
                continue;
            }
            for (SearchTourScheduleFullResponse.TourSchedulePriceResponse p : slot.getPrices()) {
                if (p.getProviderPrice() != null) {
                    p.setPrice(TouryaPriceCalculator.calculateSalePrice(p.getProviderPrice(), fraction));
                }
            }
            if (slot.getHighestPrice() != null && slot.getPrices() != null && !slot.getPrices().isEmpty()) {
                slot.getPrices().stream()
                        .map(SearchTourScheduleFullResponse.TourSchedulePriceResponse::getPrice)
                        .filter(java.util.Objects::nonNull)
                        .max(BigDecimal::compareTo)
                        .ifPresent(max -> slot.getHighestPrice().setPrice(max));
            }
        }
    }

    private void applyToSlotResponse(TourScheduleSlotResponse slot,
            Map<Integer, BigDecimal> slotFractionById,
            Map<Integer, BigDecimal> priceById,
            boolean showSlotPercentage) {
        if (slot == null) {
            return;
        }
        if (slot.getId() != null && slotFractionById.containsKey(slot.getId())) {
            if (showSlotPercentage) {
                slot.setSlotPorcentajeTourya(
                        TouryaPriceCalculator.toApiPercentPoints(slotFractionById.get(slot.getId())));
            }
        }
        if (slot.getPrices() == null) {
            return;
        }
        for (TourSchedulePriceResponse price : slot.getPrices()) {
            if (price.getId() != null && priceById.containsKey(price.getId())) {
                price.setPrice(priceById.get(price.getId()));
            } else if (slot.getId() != null && slotFractionById.containsKey(slot.getId())
                    && price.getProviderPrice() != null) {
                price.setPrice(TouryaPriceCalculator.calculateSalePrice(
                        price.getProviderPrice(), slotFractionById.get(slot.getId())));
            }
        }
    }

    private Map<Integer, Map<Integer, BigDecimal>> loadSlotFractionMaps(Collection<Integer> scheduleIds) {
        Map<Integer, Map<Integer, BigDecimal>> out = new HashMap<>();
        for (TourScheduleSlotOverride o : slotOverrideRepository.findByScheduleIdIn(scheduleIds)) {
            out.computeIfAbsent(o.getScheduleId(), k -> new HashMap<>())
                    .put(o.getSlotId(), o.getSlotPorcentajeTourya());
        }
        return out;
    }

    private Map<Integer, Map<Integer, BigDecimal>> loadPriceMaps(Collection<Integer> scheduleIds) {
        Map<Integer, Map<Integer, BigDecimal>> out = new HashMap<>();
        for (TourSchedulePriceOverride o : priceOverrideRepository.findByScheduleIdIn(scheduleIds)) {
            out.computeIfAbsent(o.getScheduleId(), k -> new HashMap<>())
                    .put(o.getPriceId(), o.getPrice());
        }
        return out;
    }
}

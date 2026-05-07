package com.tourya.api.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import com.tourya.api.services.SearchTourScheduleFullService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchTourScheduleFullServiceImpl implements SearchTourScheduleFullService {

    private final SearchTourScheduleFullRepository searchRepo;
    private final ObjectMapper objectMapper;

    @Override
    public Page<SearchTourScheduleFullResponse> searchTourSchedule(PublicTourScheduleSearchRequest filters, Pageable pageable) {
        Map<String, Object> filterMap = new HashMap<>(objectMapper.convertValue(filters, Map.class));
        return searchRepo.callStoredProcedure(filterMap, pageable)
                .map(this::enrichPriceFrom);
    }

    private SearchTourScheduleFullResponse enrichPriceFrom(SearchTourScheduleFullResponse r) {
        if (r == null || r.getTour() == null) return r;
        BigDecimal priceFrom = computePriceFrom(r.getSchedules());
        r.getTour().setPriceFrom(priceFrom);
        return r;
    }

    private BigDecimal computePriceFrom(List<SearchTourScheduleFullResponse.TourScheduleResponse> schedules) {
        if (schedules == null || schedules.isEmpty()) return null;

        BigDecimal minAdult = null;
        BigDecimal minAny = null;

        for (SearchTourScheduleFullResponse.TourScheduleResponse sch : schedules) {
            if (sch == null || sch.getConfig() == null || sch.getConfig().getSlots() == null) continue;
            for (SearchTourScheduleFullResponse.TourScheduleSlotResponse slot : sch.getConfig().getSlots()) {
                if (slot == null || slot.getPrices() == null) continue;
                for (SearchTourScheduleFullResponse.TourSchedulePriceResponse p : slot.getPrices()) {
                    if (p == null || p.getPrice() == null) continue;
                    BigDecimal price = p.getPrice();

                    minAny = (minAny == null || price.compareTo(minAny) < 0) ? price : minAny;

                    String ageType = p.getAgeType();
                    if (ageType != null && ageType.equalsIgnoreCase("ADULT")) {
                        minAdult = (minAdult == null || price.compareTo(minAdult) < 0) ? price : minAdult;
                    }
                }
            }
        }

        return minAdult != null ? minAdult : minAny;
    }
}

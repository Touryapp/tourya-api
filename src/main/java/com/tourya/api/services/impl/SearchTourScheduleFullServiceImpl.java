package com.tourya.api.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.models.responses.TourGalleryResponse;
import com.tourya.api.repository.ReviewRepository;
import com.tourya.api.repository.SearchTourScheduleFullRepository;
import com.tourya.api.services.SearchTourScheduleFullService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchTourScheduleFullServiceImpl implements SearchTourScheduleFullService {

    private final SearchTourScheduleFullRepository searchRepo;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Page<SearchTourScheduleFullResponse> searchTourSchedule(PublicTourScheduleSearchRequest filters, Pageable pageable) {
        Map<String, Object> filterMap = new HashMap<>(objectMapper.convertValue(filters, Map.class));
        Page<SearchTourScheduleFullResponse> page = searchRepo.callStoredProcedure(filterMap, pageable);
        Map<Integer, BigDecimal> avgRatingByTourId = loadAvgPublishedRatingByTourIds(page.getContent());
        return page.map(r -> enrichRating(enrichPriceFrom(enrichProfilePicture(r)), avgRatingByTourId));
    }

    private Map<Integer, BigDecimal> loadAvgPublishedRatingByTourIds(List<SearchTourScheduleFullResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Set<Integer> tourIds = new HashSet<>();
        for (SearchTourScheduleFullResponse row : rows) {
            if (row != null && row.getTour() != null && row.getTour().getId() != null) {
                tourIds.add(row.getTour().getId());
            }
        }
        if (tourIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> grouped = reviewRepository.avgPublishedRatingGroupedByTourIds(tourIds);
        Map<Integer, BigDecimal> out = new HashMap<>();
        for (Object[] row : grouped) {
            if (row == null || row.length < 2) continue;
            Integer tourId = row[0] != null ? ((Number) row[0]).intValue() : null;
            if (tourId == null) continue;
            BigDecimal avg = null;
            if (row[1] instanceof BigDecimal) {
                avg = (BigDecimal) row[1];
            } else if (row[1] instanceof Number) {
                avg = BigDecimal.valueOf(((Number) row[1]).doubleValue());
            }
            if (avg != null) {
                out.put(tourId, avg.setScale(1, RoundingMode.HALF_UP));
            }
        }
        return out;
    }

    private SearchTourScheduleFullResponse enrichRating(SearchTourScheduleFullResponse r, Map<Integer, BigDecimal> avgRatingByTourId) {
        if (r == null || r.getTour() == null || r.getTour().getId() == null) {
            return r;
        }
        BigDecimal avg = avgRatingByTourId.get(r.getTour().getId());
        if (avg != null) {
            r.getTour().setRating(avg.doubleValue());
        } else {
            r.getTour().setRating(null);
        }
        return r;
    }

    private SearchTourScheduleFullResponse enrichPriceFrom(SearchTourScheduleFullResponse r) {
        if (r == null || r.getTour() == null) return r;
        BigDecimal priceFrom = computePriceFrom(r.getSchedules());
        r.getTour().setPriceFrom(priceFrom);
        return r;
    }

    private SearchTourScheduleFullResponse enrichProfilePicture(SearchTourScheduleFullResponse r) {
        if (r == null || r.getTour() == null) return r;
        List<SearchTourScheduleFullResponse.GalleryItemResponse> gallery = r.getTour().getGallery();
        if (gallery == null || gallery.isEmpty()) return r;

        SearchTourScheduleFullResponse.GalleryItemResponse chosen = gallery.stream()
                .filter(Objects::nonNull)
                .filter(g -> g.getOrder() != null && g.getOrder() == 1)
                .findFirst()
                .orElse(gallery.get(0));

        TourGalleryResponse pic = new TourGalleryResponse();
        pic.setId(chosen.getId());
        pic.setImageUrl(chosen.getImageUrl());
        pic.setDescription(chosen.getDescription());
        pic.setOrderIndex(chosen.getOrder());
        r.getTour().setProfilePicture(pic);
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

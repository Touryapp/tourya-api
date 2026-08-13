package com.tourya.api.services.impl;

import com.tourya.api.constans.enums.AddressTypeEnum;
import com.tourya.api.models.TourAddress;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.repository.TourAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Sprint 2 mobile — deuda 2A.
 *
 * Enriquece cada fila del search con el {@code address_type} del meeting point.
 * El SP {@code sp_get_tour_schedule_json} devuelve country/state/city/address pero
 * no expone {@code address_type} — se resuelve aca en Java para evitar tocar el SP
 * (patron aditivo puro DTO + enricher).
 *
 * Estrategia:
 * 1) Batch: {@link TourAddressRepository#findByTourIdIn(List)} para todos los tour_id
 *    presentes en la pagina de resultados (1 sola query, no N+1).
 * 2) Match: por tour_id + tuple (country_id, state_id, city_id) — incluyendo el caso
 *    HOTEL_PICKUP con los 3 en null. Si hay match, se setea el enum.
 * 3) Fallback: si no matchea (deberia ser raro por FK y por el propio JOIN del SP),
 *    se deja null y el mobile cae al comportamiento previo.
 */
@Component
@RequiredArgsConstructor
public class SearchTourAddressTypeEnricher {

    private final TourAddressRepository tourAddressRepository;

    public void enrich(List<SearchTourScheduleFullResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Integer> tourIds = new HashSet<>();
        for (SearchTourScheduleFullResponse row : rows) {
            if (row != null && row.getTour() != null && row.getTour().getId() != null) {
                tourIds.add(row.getTour().getId());
            }
        }
        if (tourIds.isEmpty()) {
            return;
        }
        List<TourAddress> addresses = tourAddressRepository.findByTourIdIn(new ArrayList<>(tourIds));
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        // Map<tourId, Map<locationTupleKey, AddressType>>
        Map<Integer, Map<String, AddressTypeEnum>> byTour = new HashMap<>();
        for (TourAddress addr : addresses) {
            if (addr == null || addr.getTour() == null || addr.getTour().getId() == null) {
                continue;
            }
            Integer tourId = addr.getTour().getId();
            String key = buildKey(
                    addr.getCountry() != null ? addr.getCountry().getId() : null,
                    addr.getState() != null ? addr.getState().getId() : null,
                    addr.getCity() != null ? addr.getCity().getId() : null);
            byTour.computeIfAbsent(tourId, k -> new HashMap<>())
                    .putIfAbsent(key, addr.getAddressType());
        }
        for (SearchTourScheduleFullResponse row : rows) {
            if (row == null || row.getTour() == null || row.getTour().getAddress() == null) {
                continue;
            }
            Integer tourId = row.getTour().getId();
            SearchTourScheduleFullResponse.AddressResponse addr = row.getTour().getAddress();
            Map<String, AddressTypeEnum> byKey = byTour.get(tourId);
            if (byKey == null || byKey.isEmpty()) {
                continue;
            }
            AddressTypeEnum resolved = byKey.get(buildKey(addr.getCountry(), addr.getState(), addr.getCity()));
            if (resolved == null && byKey.size() == 1) {
                // Fallback: si el tour tiene una sola direccion, usar esa.
                resolved = byKey.values().iterator().next();
            }
            addr.setAddressType(resolved);
        }
    }

    private String buildKey(Integer country, Integer state, Integer city) {
        return Objects.toString(country, "-") + "|" + Objects.toString(state, "-") + "|" + Objects.toString(city, "-");
    }
}

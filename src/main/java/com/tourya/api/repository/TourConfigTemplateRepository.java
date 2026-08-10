package com.tourya.api.repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tourya.api.models.Provider;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.TourScheduleConfigResponse;
import com.tourya.api.services.ProviderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TourConfigTemplateRepository {


    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public TourConfigTemplateRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public List<TourScheduleConfigResponse> findByProviderId(Provider provider) {
        return findByProviderIdAndSubCategory(provider, null);
    }

    // TC-019 (#231): filtro opcional por subcategoria. Si viene null, devuelve todos los
    // templates del provider. Si viene, filtra por c.sub_category = subCategory en el SP.
    public List<TourScheduleConfigResponse> findByProviderIdAndSubCategory(Provider provider, String subCategory) {
        Integer  providerId = provider.getId();

        Query query = entityManager.createNativeQuery("SELECT get_templates_by_provider(:providerId, :subCategory)");
        query.setParameter("providerId", providerId);
        query.setParameter("subCategory", subCategory);

        List<String> results = query.getResultList();
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        try {
            return results.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, TourScheduleConfigResponse.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Error parsing JSON from SP", e);
                        }
                    })
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Error mapping stored procedure results", e);
        }
    }
}

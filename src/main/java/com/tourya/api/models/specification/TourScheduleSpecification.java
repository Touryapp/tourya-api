package com.tourya.api.models.specification;

import com.tourya.api.constans.enums.TourScheduleStatusEnum;
import com.tourya.api.models.*; // Import all models for brevity
import com.tourya.api.models.request.TourSearchRequestDto;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TourScheduleSpecification {
    public static Specification<TourSchedule> withSearchCriteria(TourSearchRequestDto request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- JOINS PRINCIPALES (los que no causan duplicados conflictivos) ---
            Join<TourSchedule, Tour> tourJoin = root.join("tour", JoinType.INNER);
            Join<TourSchedule, TourScheduleConfig> configJoin = root.join("config", JoinType.INNER); // Join config here for reuse

            // --- PREDICADOS BASE ---
            query.distinct(true);
            predicates.add(criteriaBuilder.equal(root.get("status"), TourScheduleStatusEnum.AVAILABLE));
            predicates.add(tourJoin.get("status").in("active", "accepted"));


            // --- FILTROS SOBRE TOUR (entidad principal) ---

            // 1. Filtrar por palabra clave
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String likeKeyword = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(tourJoin.get("name")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(tourJoin.get("description")), likeKeyword)
                ));
            }

            // 2. Filtrar por categoría de tour
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(tourJoin.get("tourCategory").get("id"), request.getCategoryId()));
            }

            // 3. Filtrar por edad mínima
            if (request.getMinAge() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(tourJoin.get("minAge"), request.getMinAge()));
            }

            // 4. Filtrar por rating mínimo
            if (request.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(tourJoin.get("rating"), request.getMinRating()));
            }

            // --- FILTROS SOBRE TourSchedule ---

            // 5. Filtrar por fecha específica o rango
            if (request.getSearchDate() != null) {
                predicates.add(criteriaBuilder.equal(root.get("scheduleDate"), request.getSearchDate()));
            } else {
                if (request.getStartDateRange() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("scheduleDate"), request.getStartDateRange()));
                }
                if (request.getEndDateRange() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("scheduleDate"), request.getEndDateRange()));
                }
            }

            // 6. Filtrar por rango de horas
            if (request.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), request.getStartTime()));
            }
            if (request.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), request.getEndTime()));
            }

            // 7. Filtrar por capacidad disponible
            if (request.getMinCapacityAvailable() != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isTrue(root.get("isUnlimitedCapacity")),
                        criteriaBuilder.greaterThanOrEqualTo(
                                criteriaBuilder.diff(root.get("maxCapacity"), root.get("reservedCapacity")),
                                request.getMinCapacityAvailable()
                        )
                ));
            }

            // --- FILTROS CON SUBCONSULTAS PARA EVITAR JOINS PROBLEMÁTICOS ---

            // 8. Subconsulta para filtrar por Precio (CORREGIDA)
            if (request.getMinPrice() != null || request.getMaxPrice() != null || request.getAgeType() != null) {
                Subquery<Integer> priceSubquery = query.subquery(Integer.class);
                Root<TourScheduleConfigPrice> priceRoot = priceSubquery.from(TourScheduleConfigPrice.class);
                Join<TourScheduleConfigPrice, TourScheduleConfigSlot> slotJoin = priceRoot.join("slot");

                List<Predicate> subqueryPredicates = new ArrayList<>();
                if (request.getMinPrice() != null) {
                    subqueryPredicates.add(criteriaBuilder.greaterThanOrEqualTo(priceRoot.get("price"), request.getMinPrice()));
                }
                if (request.getMaxPrice() != null) {
                    subqueryPredicates.add(criteriaBuilder.lessThanOrEqualTo(priceRoot.get("price"), request.getMaxPrice()));
                }
                if (request.getAgeType() != null) {
                    subqueryPredicates.add(criteriaBuilder.equal(priceRoot.get("ageType"), request.getAgeType()));
                }

                // Correlación correcta: el precio debe pertenecer a un slot que coincida
                // con el config_id Y el startTime del TourSchedule principal (root).
                Predicate configMatch = criteriaBuilder.equal(slotJoin.get("config").get("id"), configJoin.get("id")); // Re-use main query join
                Predicate timeMatch = criteriaBuilder.equal(slotJoin.get("startTime"), root.get("startTime"));

                priceSubquery.select(priceRoot.get("id")).where(
                    criteriaBuilder.and(subqueryPredicates.toArray(new Predicate[0])),
                    configMatch,
                    timeMatch
                );
                
                predicates.add(criteriaBuilder.exists(priceSubquery));
            }

            // 9. Subconsulta para filtrar por Ubicación
            if (request.getCountryId() != null || request.getStateId() != null || request.getCityId() != null || (request.getAddressKeyword() != null && !request.getAddressKeyword().isEmpty())) {
                Subquery<Integer> addressSubquery = query.subquery(Integer.class);
                Root<Tour> subqueryRoot = addressSubquery.from(Tour.class);
                Join<Tour, TourAddress> addressJoin = subqueryRoot.join("addresses");

                List<Predicate> subqueryPredicates = new ArrayList<>();
                if (request.getCountryId() != null) {
                    subqueryPredicates.add(criteriaBuilder.equal(addressJoin.get("country").get("id"), request.getCountryId()));
                }
                if (request.getStateId() != null) {
                    subqueryPredicates.add(criteriaBuilder.equal(addressJoin.get("state").get("id"), request.getStateId()));
                }
                if (request.getCityId() != null) {
                    subqueryPredicates.add(criteriaBuilder.equal(addressJoin.get("city").get("id"), request.getCityId()));
                }
                if (request.getAddressKeyword() != null && !request.getAddressKeyword().isEmpty()) {
                    String likeAddress = "%" + request.getAddressKeyword().toLowerCase() + "%";
                    subqueryPredicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(addressJoin.get("address")), likeAddress),
                        criteriaBuilder.like(criteriaBuilder.lower(addressJoin.get("location")), likeAddress)
                    ));
                }

                addressSubquery.select(subqueryRoot.get("id"))
                        .where(
                                criteriaBuilder.and(subqueryPredicates.toArray(new Predicate[0])),
                                criteriaBuilder.equal(subqueryRoot.get("id"), tourJoin.get("id")) // Correlaciona con el tour de la consulta principal
                        );

                predicates.add(criteriaBuilder.exists(addressSubquery));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.tourya.api.models.specification;

import com.tourya.api.models.Tour;
import com.tourya.api.models.TourAddress;
import com.tourya.api.models.TourSchedule;
import com.tourya.api.models.TourScheduleConfig;
import com.tourya.api.models.TourScheduleConfigPrice;
import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.models.request.TourSearchRequestDto;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TourScheduleSpecification {
    public static Specification<TourSchedule> withSearchCriteria(TourSearchRequestDto request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Evitar duplicados en los resultados si se usan múltiples FETCH JOINs
            // query.distinct(true); // Esto puede ser necesario si las relaciones OneToMany causan duplicados

            // Joins necesarios para acceder a las entidades relacionadas
            Join<TourSchedule, Tour> tourJoin = root.join("tour", JoinType.LEFT);
            Join<TourSchedule, TourScheduleConfig> configJoin = root.join("config", JoinType.LEFT);
            Join<TourScheduleConfig, TourScheduleConfigSlot> slotJoin = configJoin.join("slots", JoinType.LEFT);
            // Para precios, necesitamos un join condicional si se filtra por precio
            Optional<Join<TourScheduleConfigSlot, TourScheduleConfigPrice>> priceJoin = Optional.empty();
            if (request.getMinPrice() != null || request.getMaxPrice() != null || request.getAgeType() != null) {
                priceJoin = Optional.of(slotJoin.join("prices", JoinType.LEFT));
            }

            // Para direcciones, necesitamos un join condicional si se filtra por dirección
            Optional<Join<Tour, TourAddress>> addressJoin = Optional.empty();
            if (request.getCountryId() != null || request.getStateId() != null || request.getCityId() != null || request.getAddressKeyword() != null) {
                addressJoin = Optional.of(tourJoin.join("addresses", JoinType.LEFT));
            }


            // 1. Filtrar por palabra clave (tour.name o tour.description)
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String likeKeyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(tourJoin.get("name")), likeKeyword);
                Predicate descriptionLike = criteriaBuilder.like(criteriaBuilder.lower(tourJoin.get("description")), likeKeyword);
                predicates.add(criteriaBuilder.or(nameLike, descriptionLike));
            }

            // 2. Filtrar por categoría de tour
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(tourJoin.get("category").get("id"), request.getCategoryId()));
            }

            // 3. Filtrar por edad mínima del tour (min_age)
            if (request.getMinAge() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(tourJoin.get("minAge"), request.getMinAge()));
            }

            // 4. Filtrar por rating mínimo del tour
            if (request.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(tourJoin.get("rating"), request.getMinRating()));
            }

            // 5. Filtrar por fecha específica de horario (schedule_date)
            if (request.getSearchDate() != null) {
                predicates.add(criteriaBuilder.equal(root.get("scheduleDate"), request.getSearchDate()));
            }

            // 6. Filtrar por rango de fechas de disponibilidad (schedule_date)
            if (request.getStartDateRange() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("scheduleDate"), request.getStartDateRange()));
            }
            if (request.getEndDateRange() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("scheduleDate"), request.getEndDateRange()));
            }

            // 7. Filtrar por hora de inicio del slot (start_time)
            if (request.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), request.getStartTime()));
            }

            // 8. Filtrar por hora de fin del slot (end_time)
            if (request.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), request.getEndTime()));
            }

            // 9. Filtrar por capacidad disponible mínima (max_capacity - reserved_capacity)
            // Solo aplica si no es capacidad ilimitada
            if (request.getMinCapacityAvailable() != null) {
                // Si isUnlimitedCapacity es FALSE, entonces maxCapacity - reservedCapacity >= minCapacityAvailable
                // O si isUnlimitedCapacity es TRUE, no se aplica este filtro (o se considera siempre disponible)
                Predicate isNotUnlimited = criteriaBuilder.isFalse(root.get("isUnlimitedCapacity"));
                Predicate hasCapacity = criteriaBuilder.greaterThanOrEqualTo(
                        criteriaBuilder.diff(root.get("maxCapacity"), root.get("reservedCapacity")),
                        request.getMinCapacityAvailable()
                );
                // Combinar: (NO es ilimitado Y tiene capacidad) O (es ilimitado y el filtro de capacidad no importa)
                // Para simplificar, solo aplicamos el filtro si NO es ilimitado.
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isTrue(root.get("isUnlimitedCapacity")), // Si es ilimitado, siempre pasa este filtro
                        criteriaBuilder.and(isNotUnlimited, hasCapacity) // Si no es ilimitado, verifica la capacidad
                ));
            }


            // 10. Filtrar por rango de precios y tipo de edad
            if (priceJoin.isPresent()) {
                Join<TourScheduleConfigSlot, TourScheduleConfigPrice> currentPriceJoin = priceJoin.get();

                if (request.getMinPrice() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(currentPriceJoin.get("price"), request.getMinPrice()));
                }
                if (request.getMaxPrice() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(currentPriceJoin.get("price"), request.getMaxPrice()));
                }
                if (request.getAgeType() != null && !request.getAgeType().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(currentPriceJoin.get("ageType")), request.getAgeType().toLowerCase()));
                }
                // Asegurarse de que el slot y el precio estén correctamente relacionados con el horario
                // Se asume que el slot que generó el schedule tiene el mismo startTime y endTime
                predicates.add(criteriaBuilder.equal(root.get("startTime"), slotJoin.get("startTime")));
                predicates.add(criteriaBuilder.equal(root.get("endTime"), slotJoin.get("endTime")));
            }


            // 11. Filtrar por ubicación (country, state, city, address/location keyword)
            if (addressJoin.isPresent()) {
                Join<Tour, TourAddress> currentAddressJoin = addressJoin.get();

                if (request.getCountryId() != null) {
                    predicates.add(criteriaBuilder.equal(currentAddressJoin.get("country").get("id"), request.getCountryId()));
                }
                if (request.getStateId() != null) {
                    predicates.add(criteriaBuilder.equal(currentAddressJoin.get("state").get("id"), request.getStateId()));
                }
                if (request.getCityId() != null) {
                    predicates.add(criteriaBuilder.equal(currentAddressJoin.get("city").get("id"), request.getCityId()));
                }
                if (request.getAddressKeyword() != null && !request.getAddressKeyword().isEmpty()) {
                    String likeAddressKeyword = "%" + request.getAddressKeyword().toLowerCase() + "%";
                    Predicate addressLike = criteriaBuilder.like(criteriaBuilder.lower(currentAddressJoin.get("address")), likeAddressKeyword);
                    Predicate locationLike = criteriaBuilder.like(criteriaBuilder.lower(currentAddressJoin.get("location")), likeAddressKeyword);
                    predicates.add(criteriaBuilder.or(addressLike, locationLike));
                }
            }

            // Asegurar que el tour schedule esté "available" por defecto, a menos que se especifique lo contrario
            predicates.add(criteriaBuilder.equal(root.get("status"), "available"));


            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

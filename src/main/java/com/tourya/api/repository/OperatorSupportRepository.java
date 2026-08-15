package com.tourya.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * IA-07: consultas nativas dedicadas al agente Operator Support. Se aisla del
 * {@link TourRepository} para que la logica especifica del agente (comparables
 * de pricing) no contamine el repositorio principal — mismo patron que
 * {@link TourTagRepository}.
 *
 * <p>Todas las queries devuelven precios publicos ({@code tour_schedule_config_price.price})
 * — nunca el {@code provider_price} interno. Comparar public price vs public
 * price mantiene la privacidad del margen entre proveedores y a la vez es
 * util para el operador (el turista ve public price al final).</p>
 */
@Repository
public class OperatorSupportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Precio publico promedio (ADULT) del tour indicado, calculado en BD para
     * evitar traer todas las filas.
     *
     * @param tourId ID del tour bajo analisis.
     * @return promedio como {@link BigDecimal}, o {@code null} si el tour no
     *         tiene precios ADULT configurados.
     */
    public BigDecimal findAvgAdultPriceByTourId(Integer tourId) {
        Object result = entityManager.createNativeQuery("""
                SELECT AVG(p.price)
                FROM tour_schedule_config c
                JOIN tour_schedule_config_slot s ON s.config_id = c.id
                JOIN tour_schedule_config_price p ON p.slot_id = s.id
                WHERE c.tour_id = :tourId
                  AND p.age_type = 'ADULT'
                """)
                .setParameter("tourId", tourId)
                .getSingleResult();
        return result == null ? null : toBigDecimal(result);
    }

    /**
     * Precios promedio ADULT de tours comparables — misma subcategoria, distintos
     * del tour actual, {@code status='accepted'}, limitado por {@code limit}.
     *
     * <p>Devuelve una lista plana de precios (uno por tour) ordenados ascendentemente
     * — el service calcula min/max/median en Java.</p>
     *
     * @param subCategory Valor del enum {@code tour_subcategory_enum} (ej. "snorkeling").
     * @param excludeTourId Tour bajo analisis (se excluye para no auto-comparar).
     * @param limit Tope superior de tours a considerar (defensivo, evita cargar todo).
     */
    @SuppressWarnings("unchecked")
    public List<BigDecimal> findComparableAdultPricesBySubcategory(
            String subCategory, Integer excludeTourId, int limit) {
        List<Object> raw = entityManager.createNativeQuery("""
                SELECT AVG(p.price) AS avg_price
                FROM tour t
                JOIN tour_schedule_config c ON c.tour_id = t.id
                JOIN tour_schedule_config_slot s ON s.config_id = c.id
                JOIN tour_schedule_config_price p ON p.slot_id = s.id
                WHERE t.status = 'accepted'
                  AND t.sub_category = CAST(:subCategory AS tour_subcategory_enum)
                  AND t.id <> :excludeTourId
                  AND p.age_type = 'ADULT'
                GROUP BY t.id
                HAVING AVG(p.price) IS NOT NULL
                ORDER BY AVG(p.price) ASC
                """)
                .setParameter("subCategory", subCategory)
                .setParameter("excludeTourId", excludeTourId)
                .setMaxResults(Math.max(1, limit))
                .getResultList();

        return raw.stream()
                .map(OperatorSupportRepository::toBigDecimal)
                .toList();
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }
}

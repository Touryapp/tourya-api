package com.tourya.api.repository.impl;

import com.tourya.api.models.mapper.ReservationDetailsMapper;
import com.tourya.api.models.responses.ReservationDetailsResponse;
import com.tourya.api.repository.ReservationNativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReservationNativeRepositoryImpl implements ReservationNativeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ReservationDetailsMapper mapper;

    @Override
    public List<ReservationDetailsResponse> getProviderReservations(
            Integer providerId,
            Long reservationId,
            String deliveryStatus,
            int page,
            int size
    ) {
        int offset = page * size;

        String sql = """
            SELECT *
            FROM sp_get_provider_reservations(?, ?, ?)
            ORDER BY reservationdate DESC
            LIMIT ? OFFSET ?
            """;

        log.info("Executing SQL getProviderReservations providerId={}, reservationId={}, status={}, page={}, size={}",
                providerId, reservationId, deliveryStatus, page, size);

        try {
            // Primero verificar cuántas filas devuelve el stored procedure sin mapear
            String countSql = """
                SELECT COUNT(*) 
                FROM sp_get_provider_reservations(?, ?, ?)
                """;
            Integer rawCount = jdbcTemplate.queryForObject(
                    countSql,
                    Integer.class,
                    providerId,
                    reservationId,
                    deliveryStatus
            );
            log.info("Raw stored procedure returned {} rows (before mapping)", rawCount);
            
            // Verificar la estructura del stored procedure en la base de datos
            try {
                String checkSpSql = """
                    SELECT pg_get_functiondef(oid) as function_def
                    FROM pg_proc 
                    WHERE proname = 'sp_get_provider_reservations'
                    LIMIT 1
                    """;
                String spDefinition = jdbcTemplate.queryForObject(checkSpSql, String.class);
                if (spDefinition != null) {
                    boolean hasMinCapacity = spDefinition.contains("min_capacity");
                    boolean hasMaxCapacity = spDefinition.contains("max_capacity");
                    log.info("Stored procedure in DB - has min_capacity: {}, has max_capacity: {}", 
                            hasMinCapacity, hasMaxCapacity);
                }
            } catch (Exception e) {
                log.warn("Could not check stored procedure definition: {}", e.getMessage());
            }
            
            // Ejecutar una consulta directa para verificar si hay datos
            String testSql = """
                SELECT reservationid, tourproviderid 
                FROM sp_get_provider_reservations(?, ?, ?)
                LIMIT 5
                """;
            try {
                List<Object[]> testResults = jdbcTemplate.query(
                        testSql,
                        (rs, rowNum) -> new Object[]{rs.getLong("reservationid"), rs.getObject("tourproviderid")},
                        providerId,
                        reservationId,
                        deliveryStatus
                );
                log.info("Direct query test returned {} rows. Sample: {}", testResults.size(), 
                        testResults.isEmpty() ? "none" : testResults.get(0));
            } catch (Exception e) {
                log.error("Error in direct query test: {}", e.getMessage(), e);
            }
            
            List<ReservationDetailsResponse> result = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> {
                        try {
                            log.debug("Mapping row {} - reservationId: {}", rowNum, rs.getLong("reservationid"));
                            return mapRow(rs);
                        } catch (Exception e) {
                            log.error("Error mapping row {}: {}", rowNum, e.getMessage(), e);
                            throw new RuntimeException("Error mapping reservation row: " + e.getMessage(), e);
                        }
                    },
                    providerId,
                    reservationId,
                    deliveryStatus,
                    size,
                    offset
            );
            log.info("Query returned {} results after mapping", result.size());
            return result;
        } catch (Exception e) {
            log.error("Error executing getProviderReservations: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public long countProviderReservations(Integer providerId, Long reservationId, String deliveryStatus) {
        String sql = """
            SELECT COUNT(*) 
            FROM sp_get_provider_reservations(?, ?, ?)
            """;

        Long result = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                providerId,
                reservationId,
                deliveryStatus
        );
        return result != null ? result : 0L;
    }

    @Override
    public void deleteShoppingCartItemDirectly(Long itemId) {
        // Primero eliminar los detalles del item
        jdbcTemplate.update("DELETE FROM shopping_cart_item_detail WHERE shopping_cart_item_id = ?", itemId);
        
        // Luego eliminar el item
        // La foreign key constraint debería tener ON DELETE SET NULL para que item_id se ponga en NULL
        // automáticamente cuando se elimina el item. Si no está configurado así, necesitas ejecutar
        // la migración 012_modify_reservation_item_id_constraint.sql
        jdbcTemplate.update("DELETE FROM shopping_cart_item WHERE id = ?", itemId);
        
        log.info("Deleted shopping cart item {} directly using SQL (item_id in reservations will be set to NULL by constraint)", itemId);
    }

    @Override
    public void updateReservationItemIdToNull(Long reservationId) {
        // Actualizar item_id a NULL para liberar la foreign key constraint
        // Esto permite limpiar el carrito normalmente
        jdbcTemplate.update("UPDATE reservation SET item_id = NULL WHERE reservation_id = ?", reservationId);
        log.info("Updated reservation {} to set item_id = NULL", reservationId);
    }

    /** Delegamos el mapeo al mapper de Spring */
    private ReservationDetailsResponse mapRow(ResultSet rs) throws SQLException {
        return mapper.map(rs);
    }
}

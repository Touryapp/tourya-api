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
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReservationNativeRepositoryImpl implements ReservationNativeRepository {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "reservationdate",
            "reservationcreateddate",
            "scheduledate"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ReservationDetailsMapper mapper;

    @Override
    public List<ReservationDetailsResponse> getProviderReservations(
            Integer providerId,
            Integer customerUserId,
            Long reservationId,
            String deliveryStatus,
            String subCategory,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        int offset = page * size;
        String orderColumn = resolveSortColumn(sortBy);
        String orderDir = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

        String sql = """
            SELECT *
            FROM sp_get_provider_reservations(?, ?, ?, ?, ?)
            ORDER BY %s %s
            LIMIT ? OFFSET ?
            """.formatted(orderColumn, orderDir);

        log.info("getProviderReservations providerId={}, customerUserId={}, reservationId={}, status={}, subCategory={}, sort={} {}, page={}, size={}",
                providerId, customerUserId, reservationId, deliveryStatus, subCategory, orderColumn, orderDir, page, size);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs),
                providerId,
                customerUserId,
                reservationId,
                deliveryStatus,
                subCategory,
                size,
                offset
        );
    }

    @Override
    public long countProviderReservations(
            Integer providerId,
            Integer customerUserId,
            Long reservationId,
            String deliveryStatus,
            String subCategory) {
        String sql = """
            SELECT COUNT(*)
            FROM sp_get_provider_reservations(?, ?, ?, ?, ?)
            """;

        Long result = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                providerId,
                customerUserId,
                reservationId,
                deliveryStatus,
                subCategory
        );
        return result != null ? result : 0L;
    }

    @Override
    public void deleteShoppingCartItemDirectly(Long itemId) {
        jdbcTemplate.update("DELETE FROM shopping_cart_item_detail WHERE shopping_cart_item_id = ?", itemId);
        jdbcTemplate.update("DELETE FROM shopping_cart_item WHERE id = ?", itemId);
        log.info("Deleted shopping cart item {} directly using SQL", itemId);
    }

    @Override
    public void updateReservationItemIdToNull(Long reservationId) {
        jdbcTemplate.update("UPDATE reservation SET item_id = NULL WHERE reservation_id = ?", reservationId);
        log.info("Updated reservation {} to set item_id = NULL", reservationId);
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "reservationdate";
        }
        String normalized = switch (sortBy.trim()) {
            case "scheduleDate" -> "scheduledate";
            case "reservationCreatedDate" -> "reservationcreateddate";
            case "reservationDate" -> "reservationdate";
            default -> sortBy.trim().toLowerCase();
        };
        if (!ALLOWED_SORT_COLUMNS.contains(normalized)) {
            return "reservationdate";
        }
        return normalized;
    }

    private ReservationDetailsResponse mapRow(ResultSet rs) throws SQLException {
        return mapper.map(rs);
    }
}

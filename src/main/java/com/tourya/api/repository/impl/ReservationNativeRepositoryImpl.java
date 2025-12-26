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

        return jdbcTemplate.query(
                sql,
                new Object[]{
                        providerId,
                        reservationId,
                        deliveryStatus,
                        size,
                        offset
                },
                (rs, rowNum) -> mapRow(rs)
        );
    }

    @Override
    public long countProviderReservations(Integer providerId, Long reservationId, String deliveryStatus) {
        String sql = """
            SELECT COUNT(*) 
            FROM sp_get_provider_reservations(?, ?, ?)
            """;

        return jdbcTemplate.queryForObject(
                sql,
                new Object[]{providerId, reservationId, deliveryStatus},
                Long.class
        );
    }

    /** Delegamos el mapeo al mapper de Spring */
    private ReservationDetailsResponse mapRow(ResultSet rs) throws SQLException {
        return mapper.map(rs);
    }
}

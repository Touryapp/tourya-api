package com.tourya.api.models.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourya.api._utils.ReservationDisplayId;
import com.tourya.api.models.TranslatedField;
import com.tourya.api.models.responses.ReservationDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class ReservationDetailsMapper {

    private final ObjectMapper objectMapper;

    private static String readOptionalString(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException ex) {
            return null;
        }
    }

    private static Double readOptionalDouble(ResultSet rs, String column) throws SQLException {
        try {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException ex) {
            return null;
        }
    }

    public ReservationDetailsResponse map(ResultSet rs) throws SQLException {
        // Parse JSONB tourname to TranslatedField
        TranslatedField tourName = null;
        String tourNameJson = rs.getString("tourname");
        if (tourNameJson != null) {
            try {
                tourName = objectMapper.readValue(tourNameJson, TranslatedField.class);
            } catch (Exception e) {
                // If parsing fails, create a TranslatedField with the raw string
                tourName = TranslatedField.ofSpanish(tourNameJson);
            }
        }

        long reservationId = rs.getLong("reservationid");
        return ReservationDetailsResponse.builder()
                .reservationId(reservationId)
                .bookingId(ReservationDisplayId.format(reservationId))
                .reservationDate(rs.getString("reservationdate"))
                .reservationDeliveryStatus(rs.getString("reservationdeliverystatus"))
                .reservationCreatedDate(rs.getString("reservationcreateddate"))

                .paymentId(rs.getLong("paymentid"))
                .paymentTransactionId(rs.getString("paymenttransactionid"))
                .payerName(rs.getString("payername"))
                .payerEmail(rs.getString("payeremail"))
                .payerPhone(rs.getString("payerphone"))
                .payerDocumentType(rs.getString("payerdocumenttype"))
                .payerDocumentNumber(rs.getString("payerdocumentnumber"))

                .shoppingItemId(rs.getInt("shoppingitemid"))
                .shoppingTotalPrice(rs.getDouble("shoppingtotalprice"))
                .shoppingUnitPrice(rs.getDouble("shoppingunitprice"))
                .providerPrice(readOptionalDouble(rs, "providerprice"))
                .shoppingQuantity(rs.getInt("shoppingquantity"))

                .productType(rs.getString("producttype"))
                .productId(rs.getInt("productid"))

                .totalTourists(rs.getLong("totaltourists")) // BIGINT OK ✔

                .tourId(rs.getInt("tourid"))
                .tourName(tourName)
                .tourCategoryId(rs.getInt("tourcategoryid"))
                .tourSubCategory(readOptionalString(rs, "toursubcategory"))
                .tourProviderId(rs.getInt("tourproviderid"))

                .tourScheduleId(rs.getInt("tourscheduleid"))
                .scheduleDate(rs.getString("scheduledate"))

                .slotId(rs.getInt("slotid"))
                .slotTimeStart(rs.getString("slottime_start"))
                .slotTimeEnd(rs.getString("slottime_end"))

                // minCapacity y maxCapacity eliminados - la capacidad ahora se maneja a nivel de TourSchedule
                // .minCapacity(rs.getInt("min_capacity"))
                // .maxCapacity(rs.getInt("max_capacity"))

                .serviceResponsibleName(rs.getString("service_responsible_name"))
                .serviceResponsibleEmail(rs.getString("service_responsible_email"))
                .serviceResponsiblePhone(rs.getString("service_responsible_phone"))

                // Campos de cancelación y re-agendamiento
                .maxCancellationDate(rs.getDate("max_cancellation_date") != null 
                        ? rs.getDate("max_cancellation_date").toLocalDate() : null)
                .maxReschedulingDate(rs.getDate("max_rescheduling_date") != null 
                        ? rs.getDate("max_rescheduling_date").toLocalDate() : null)
                .cancellationReason(rs.getString("cancellation_reason"))
                .cancellationDate(rs.getTimestamp("cancellation_date") != null 
                        ? rs.getTimestamp("cancellation_date").toLocalDateTime() : null)

                .build();
    }
}

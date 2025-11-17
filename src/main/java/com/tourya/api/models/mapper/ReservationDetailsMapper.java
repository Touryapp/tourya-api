package com.tourya.api.models.mapper;

import com.tourya.api.models.responses.ReservationDetailsResponse;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ReservationDetailsMapper {

    public ReservationDetailsResponse map(ResultSet rs) throws SQLException {
        return ReservationDetailsResponse.builder()
                .reservationId(rs.getLong("reservationid"))
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
                .shoppingQuantity(rs.getInt("shoppingquantity"))

                .productType(rs.getString("producttype"))
                .productId(rs.getInt("productid"))

                .totalTourists(rs.getLong("totaltourists")) // BIGINT OK ✔

                .tourId(rs.getInt("tourid"))
                .tourName(rs.getString("tourname"))
                .tourCategoryId(rs.getInt("tourcategoryid"))
                .tourProviderId(rs.getInt("tourproviderid"))

                .tourScheduleId(rs.getInt("tourscheduleid"))
                .scheduleDate(rs.getString("scheduledate"))

                .slotId(rs.getInt("slotid"))
                .slotTimeStart(rs.getString("slottime_start"))
                .slotTimeEnd(rs.getString("slottime_end"))

                .minCapacity(rs.getInt("min_capacity"))
                .maxCapacity(rs.getInt("max_capacity"))

                .serviceResponsibleName(rs.getString("service_responsible_name"))
                .serviceResponsibleEmail(rs.getString("service_responsible_email"))
                .serviceResponsiblePhone(rs.getString("service_responsible_phone"))

                .build();
    }
}

package com.tourya.api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDetailsResponse {
    private Integer id;
    private String reservationId;
    private Long paymentId;
    private String transactionId;
    private String payer;
    private String email;
    private LocalDateTime reservationDate;
    private String status;
    private Integer tourId;
    private String tourName;
    private String tourType;
    private Double price;
    private String travellers;
    private String duration;
    private LocalDateTime checkInDate;
    private LocalDateTime returnDate;
    private String destination;
    private String customerPhone;
    private List<String> extraServices;
    private List<String> activities;
}


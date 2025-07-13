package com.tourya.api.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequest {

    @NotNull(message = "Schedule ID cannot be null")
    private Integer scheduleId;

    @NotEmpty(message = "Reservation must contain at least one item")
    @Valid // This annotation ensures that the items in the list are also validated
    private List<ReservationItemRequest> items;

    @NotEmpty(message = "Client name cannot be empty")
    private String clientName;

    @NotEmpty(message = "Client email cannot be empty")
    @Email(message = "Client email should be valid")
    private String clientEmail;

    private String clientPhone;

    private String paymentMethod;

    @NotEmpty(message = "Currency cannot be empty")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    private String currency;
}

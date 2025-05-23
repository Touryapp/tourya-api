package com.tourya.api.models.resquest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourCreateRequest {

    @NotNull(message = "Tour details are mandatory") // Opcional, si tourRequest no puede ser null
    @Valid
    private TourRequest tourRequest;

    @NotNull(message = "Tour address details are mandatory") // Opcional, si tourAddressRequest no puede ser null
    @Valid
    private TourAddressRequest tourAddressRequest;
}

package com.tourya.api.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** MO-40: body de DELETE /users/device-token — se envía al logout. */
@Data
public class UnregisterDeviceTokenRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "Token FCM a desregistrar")
    private String token;
}

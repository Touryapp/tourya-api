package com.tourya.api.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * MO-40: body de POST /users/device-token para registrar un dispositivo del
 * usuario logueado. La app mobile lo llama al login y cuando el sistema le
 * entrega un token FCM nuevo.
 */
@Data
public class RegisterDeviceTokenRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "Token FCM/APNs entregado por Firebase Messaging al cliente")
    private String token;

    @NotBlank
    @Pattern(regexp = "ANDROID|IOS|WEB", message = "platform must be ANDROID, IOS or WEB")
    @Schema(description = "Plataforma del dispositivo", example = "ANDROID", allowableValues = {"ANDROID", "IOS", "WEB"})
    private String platform;
}

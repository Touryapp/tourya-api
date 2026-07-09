package com.tourya.api.config.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken is mandatory")
    private String refreshToken;
}

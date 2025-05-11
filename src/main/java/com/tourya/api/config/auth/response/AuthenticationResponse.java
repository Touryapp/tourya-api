package com.tourya.api.config.auth.response;

import com.tourya.api.models.responses.MetaResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    private MetaResponse meta = new MetaResponse();
    private String fullName;
    private String email;
    private String token;
}

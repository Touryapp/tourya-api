package com.tourya.api.config.auth.response;

import com.tourya.api.models.Role;
import com.tourya.api.models.responses.MetaResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    private MetaResponse meta = new MetaResponse();
    private String fullName;
    private String email;
    private List<Role> roleList;
    private String token;
}

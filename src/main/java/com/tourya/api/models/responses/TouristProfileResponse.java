package com.tourya.api.models.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TouristProfileResponse {
    private Long id;
    private Integer userId;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String phone;
    private String email;
    private String city;
    private String state;
    private String country;
    private String photoUrl;
}


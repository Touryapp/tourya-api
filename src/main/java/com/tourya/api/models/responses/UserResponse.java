package com.tourya.api.models.responses;

import lombok.Data;

@Data
public class UserResponse {
    private Integer id;
    private String firstName;
    private String lastName;
    private String fullName;
    private boolean accountLocked;
    private boolean enabled;
}

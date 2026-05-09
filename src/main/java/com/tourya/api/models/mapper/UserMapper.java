package com.tourya.api.models.mapper;

import com.tourya.api.models.User;
import com.tourya.api.models.responses.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class UserMapper {
    public UserResponse toUserResponse(User user){
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setFirstName(user.getFirstname());
        userResponse.setLastName(user.getLastname());
        userResponse.setFullName(user.fullName());
        userResponse.setEnabled(user.isEnabled());
        userResponse.setAccountLocked(user.isAccountLocked());
        return userResponse;
    }
}

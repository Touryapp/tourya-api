package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Role;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.UserMapper;
import com.tourya.api.models.request.ChangePasswordRequest;
import com.tourya.api.models.responses.UserResponse;
import com.tourya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";
    public void changePassword(ChangePasswordRequest changePasswordRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());

        //check if the current password is correct
        if(!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())){
            throw new OperationNotPermittedException("Wrong password");
        }
        //check if the two password are the same
        if(!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmationPassword())){
            throw new OperationNotPermittedException("Password are not the same");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
    }

    public PageResponse<UserResponse> findAll(int page, int size, String firstName, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<User>  allUsers =  userRepository.findAllUser(firstName, pageable);

            List<UserResponse> userResponseList = allUsers.stream()
                    .map(userMapper::toUserResponse)
                    .toList();

            return new PageResponse<>(
                    userResponseList,
                    allUsers.getNumber(),
                    allUsers.getSize(),
                    allUsers.getTotalElements(),
                    allUsers.getTotalPages(),
                    allUsers.isFirst(),
                    allUsers.isLast()
            );
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    public UserResponse consultDataById(Integer userId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<User> userOptional  = userRepository.findById(userId);
            if(userOptional.isPresent()){
                return userMapper.toUserResponse(userOptional.get());
            }else{
                throw new ResourceNotFoundException("user not found Id: "+userId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public void blockById(Integer userId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<User> userOptional  = userRepository.findById(userId);
            if(userOptional.isPresent()){
                User userFond = userOptional.get();
                userFond.setAccountLocked(Boolean.TRUE);
                userRepository.save(userFond);
            }else{
                throw new ResourceNotFoundException("user not found Id: "+userId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public void unBlockById(Integer userId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<User> userOptional  = userRepository.findById(userId);
            if(userOptional.isPresent()){
                User userFond = userOptional.get();
                userFond.setAccountLocked(Boolean.FALSE);
                userRepository.save(userFond);
            }else{
                throw new ResourceNotFoundException("user not found Id: "+userId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
}

package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Role;
import com.tourya.api.models.User;
import com.tourya.api.models.request.CreateBackofficeUserRequest;
import com.tourya.api.models.request.ResetBackofficeUserPasswordRequest;
import com.tourya.api.models.request.UpdateBackofficeUserRequest;
import com.tourya.api.models.responses.BackofficeUserResponse;
import com.tourya.api.repository.RoleRepository;
import com.tourya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * FE-15d: CRUD de usuarios con rol {@code BACKOFFICE_OPERATION}. Solo ADMIN puede
 * gestionarlos. Reusa el patron de {@link ProviderUserService#createOperator} pero
 * sin la relacion provider/tour (los BACKOFFICE_OPERATION son staff Tourya).
 */
@Service
@RequiredArgsConstructor
public class BackofficeUserService {

    private static final String ROLE_NAME = "BACKOFFICE_OPERATION";
    private static final String NOT_PRIVILEGES =
            "You have no privileges to perform this action.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<BackofficeUserResponse> list(Authentication connectedUser) {
        requireAdmin(connectedUser);
        return userRepository.findByRoleName(ROLE_NAME).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BackofficeUserResponse create(CreateBackofficeUserRequest request,
                                         Authentication connectedUser) {
        requireAdmin(connectedUser);

        String email = request.getEmail().trim().toLowerCase();
        if (!Utils.isValidEmail(email)) {
            throw new OperationNotPermittedException("Invalid email format.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new OperationNotPermittedException("Email already registered.");
        }

        Role backofficeRole = roleRepository.findByName(ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE " + ROLE_NAME + " was not initiated"));

        User user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(email)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .password(passwordEncoder.encode(request.getTemporaryPassword()))
                .enabled(true)
                .mustChangePassword(true)
                .accountLocked(false)
                .roles(new ArrayList<>(List.of(backofficeRole)))
                .build();
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public BackofficeUserResponse update(Integer userId,
                                         UpdateBackofficeUserRequest request,
                                         Authentication connectedUser) {
        requireAdmin(connectedUser);
        User user = requireBackofficeUser(userId);

        if (request.getFirstname() != null) {
            user.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            user.setLastname(request.getLastname());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void resetTemporaryPassword(Integer userId,
                                       ResetBackofficeUserPasswordRequest request,
                                       Authentication connectedUser) {
        requireAdmin(connectedUser);
        User user = requireBackofficeUser(userId);
        user.setPassword(passwordEncoder.encode(request.getTemporaryPassword()));
        user.setMustChangePassword(true);
        userRepository.save(user);
    }

    @Transactional
    public void toggleEnabled(Integer userId, boolean enabled,
                              Authentication connectedUser) {
        requireAdmin(connectedUser);
        User user = requireBackofficeUser(userId);
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    private User requireBackofficeUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
        boolean isBackoffice = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> ROLE_NAME.equalsIgnoreCase(r.getName()));
        if (!isBackoffice) {
            throw new OperationNotPermittedException(
                    "User with id " + userId + " is not a BACKOFFICE_OPERATION.");
        }
        return user;
    }

    private void requireAdmin(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isAdmin(user.getRoles())) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    private BackofficeUserResponse toResponse(User user) {
        return BackofficeUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.fullName())
                .phone(user.getPhone())
                .accountEnabled(user.isEnabled())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }
}

package com.tourya.api.services;



import com.tourya.api._utils.Utils;

import com.tourya.api.exceptions.InsufficientPrivilegesException;

import com.tourya.api.exceptions.OperationNotPermittedException;

import com.tourya.api.exceptions.ResourceNotFoundException;

import com.tourya.api.models.Provider;

import com.tourya.api.models.ProviderUser;

import com.tourya.api.models.ProviderUserTour;

import com.tourya.api.models.Role;

import com.tourya.api.models.Tour;

import com.tourya.api.models.User;

import com.tourya.api.models.request.CreateProviderOperatorRequest;

import com.tourya.api.models.request.ResetProviderOperatorPasswordRequest;

import com.tourya.api.models.request.UpdateProviderOperatorRequest;

import com.tourya.api.models.responses.ProviderOperatorResponse;

import com.tourya.api.models.responses.ProviderOperatorTourResponse;

import com.tourya.api.repository.ProviderUserRepository;

import com.tourya.api.repository.ProviderUserTourRepository;

import com.tourya.api.repository.RoleRepository;

import com.tourya.api.repository.TourRepository;

import com.tourya.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.List;

import java.util.Set;

import java.util.stream.Collectors;



@Slf4j

@Service

@RequiredArgsConstructor

public class ProviderUserService {



    private final ProviderUserRepository providerUserRepository;

    private final ProviderUserTourRepository providerUserTourRepository;

    private final ProviderService providerService;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final TourRepository tourRepository;

    private final PasswordEncoder passwordEncoder;



    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";



    @Transactional(readOnly = true)

    public List<ProviderOperatorResponse> listOperators(Authentication connectedUser) {

        Provider provider = requireProviderOwner(connectedUser);

        return providerUserRepository.findByProviderId(provider.getId()).stream()

                .map(this::toResponse)

                .toList();

    }



    @Transactional

    public ProviderOperatorResponse createOperator(CreateProviderOperatorRequest request,

            Authentication connectedUser) {

        Provider provider = requireProviderOwner(connectedUser);



        String email = request.getEmail().trim().toLowerCase();

        if (!Utils.isValidEmail(email)) {

            throw new OperationNotPermittedException("Invalid email format.");

        }

        if (userRepository.existsByEmail(email)) {

            throw new OperationNotPermittedException("Email already registered.");

        }



        Role operatorRole = roleRepository.findByName("PROVIDER_OPERATOR")

                .orElseThrow(() -> new IllegalStateException("ROLE PROVIDER_OPERATOR was not initiated"));



        User user = User.builder()

                .firstname(request.getFirstname())

                .lastname(request.getLastname())

                .email(email)

                .password(passwordEncoder.encode(request.getTemporaryPassword()))

                .enabled(true)

                .mustChangePassword(true)

                .accountLocked(false)

                .roles(new ArrayList<>(List.of(operatorRole)))

                .build();

        user = userRepository.save(user);



        ProviderUser providerUser = ProviderUser.builder()

                .provider(provider)

                .user(user)

                .isPrimary(false)

                .build();

        providerUser = providerUserRepository.save(providerUser);



        assignTours(providerUser, provider, request.getTourIds(), request.getPrincipalTourId());



        return toResponse(providerUser);

    }



    @Transactional

    public ProviderOperatorResponse updateOperator(Integer providerUserId,

            UpdateProviderOperatorRequest request,

            Authentication connectedUser) {

        Provider provider = requireProviderOwner(connectedUser);

        ProviderUser providerUser = requireOperatorOfProvider(providerUserId, provider);



        User user = providerUser.getUser();

        if (Boolean.TRUE.equals(providerUser.getIsPrimary())) {

            throw new OperationNotPermittedException("Cannot update the primary provider account via this endpoint.");

        }



        if (request.getFirstname() != null && !request.getFirstname().isBlank()) {

            user.setFirstname(request.getFirstname().trim());

        }

        if (request.getLastname() != null) {

            user.setLastname(request.getLastname().trim());

        }

        userRepository.save(user);



        if (request.getTourIds() != null) {

            if (request.getTourIds().isEmpty()) {

                throw new OperationNotPermittedException("tourIds cannot be empty.");

            }

            providerUserTourRepository.deleteByProviderUserId(providerUser.getId());

            assignTours(providerUser, provider, request.getTourIds(), request.getPrincipalTourId());

        } else if (request.getPrincipalTourId() != null) {

            setPrincipalTour(providerUser, provider, request.getPrincipalTourId());

        }



        return toResponse(providerUserRepository.findByIdWithUserAndProvider(providerUserId).orElse(providerUser));

    }



    @Transactional

    public ProviderOperatorResponse updatePrincipalTour(Integer providerUserId, Integer tourId,

            Authentication connectedUser) {

        Provider provider = requireProviderOwner(connectedUser);

        ProviderUser providerUser = requireOperatorOfProvider(providerUserId, provider);

        if (Boolean.TRUE.equals(providerUser.getIsPrimary())) {

            throw new OperationNotPermittedException("Primary provider account has no tour assignment.");

        }

        setPrincipalTour(providerUser, provider, tourId);

        return toResponse(providerUserRepository.findByIdWithUserAndProvider(providerUserId).orElse(providerUser));

    }



    @Transactional

    public void resetTemporaryPassword(Integer providerUserId,

            ResetProviderOperatorPasswordRequest request,

            Authentication connectedUser) {

        Provider provider = requireProviderOwner(connectedUser);

        ProviderUser providerUser = requireOperatorOfProvider(providerUserId, provider);

        if (Boolean.TRUE.equals(providerUser.getIsPrimary())) {

            throw new OperationNotPermittedException("Cannot reset password for the primary provider account.");

        }

        User user = providerUser.getUser();

        user.setPassword(passwordEncoder.encode(request.getTemporaryPassword()));

        user.setMustChangePassword(true);

        user.setEnabled(true);

        userRepository.save(user);

    }



    private void setPrincipalTour(ProviderUser providerUser, Provider provider, Integer tourId) {

        Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());

        if (tour == null) {

            throw new ResourceNotFoundException("Tour not found for provider. tourId=" + tourId);

        }

        ProviderUserTour link = providerUserTourRepository

                .findByProviderUserIdAndTourId(providerUser.getId(), tourId)

                .orElseThrow(() -> new OperationNotPermittedException(

                        "Operator is not assigned to tour " + tourId + ". Assign the tour first."));

        providerUserTourRepository.clearPrincipalForTour(tourId);

        link.setIsPrincipal(true);

        providerUserTourRepository.save(link);

    }



    private void assignTours(ProviderUser providerUser, Provider provider, List<Integer> tourIds,

            Integer principalTourId) {

        Set<Integer> uniqueTourIds = tourIds.stream().collect(Collectors.toSet());

        if (principalTourId != null && !uniqueTourIds.contains(principalTourId)) {

            throw new OperationNotPermittedException("principalTourId must be included in tourIds.");

        }

        for (Integer tourId : uniqueTourIds) {

            Tour tour = tourRepository.findTourByIdAndProviderId(tourId, provider.getId());

            if (tour == null) {

                throw new ResourceNotFoundException("Tour not found for provider. tourId=" + tourId);

            }

            if (principalTourId != null && principalTourId.equals(tourId)) {

                providerUserTourRepository.clearPrincipalForTour(tourId);

            }

            ProviderUserTour link = ProviderUserTour.builder()

                    .providerUser(providerUser)

                    .tour(tour)

                    .isPrincipal(principalTourId != null && principalTourId.equals(tourId))

                    .createdDate(LocalDateTime.now())

                    .build();

            providerUserTourRepository.save(link);

        }

    }



    private ProviderUser requireOperatorOfProvider(Integer providerUserId, Provider provider) {

        ProviderUser providerUser = providerUserRepository.findByIdWithUserAndProvider(providerUserId)

                .orElseThrow(() -> new ResourceNotFoundException("Operator not found. id=" + providerUserId));

        if (!providerUser.getProvider().getId().equals(provider.getId())) {

            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);

        }

        return providerUser;

    }



    private Provider requireProviderOwner(Authentication connectedUser) {

        User user = (User) connectedUser.getPrincipal();

        if (!Utils.isProvider(user.getRoles())) {

            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);

        }

        return providerService.findByUserAndStatusActive(user);

    }



    private ProviderOperatorResponse toResponse(ProviderUser providerUser) {

        User user = providerUser.getUser();

        List<ProviderOperatorTourResponse> tours = providerUserTourRepository

                .findByProviderUserIdWithTour(providerUser.getId()).stream()

                .map(link -> ProviderOperatorTourResponse.builder()

                        .tourId(link.getTour().getId())

                        .tourName(link.getTour().getName() != null ? link.getTour().getName().getEs() : null)

                        .isPrincipal(link.getIsPrincipal())

                        .build())

                .toList();



        return ProviderOperatorResponse.builder()

                .providerUserId(providerUser.getId())

                .userId(user.getId())

                .email(user.getEmail())

                .fullName(user.fullName())

                .isPrimary(providerUser.getIsPrimary())

                .accountEnabled(user.isEnabled())

                .mustChangePassword(user.isMustChangePassword())

                .tours(tours)

                .build();

    }

}



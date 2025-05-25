package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.TourIncludesExcludesMapper;
import com.tourya.api.models.resquest.TourIncludesExcludesRequest;
import com.tourya.api.models.responses.TourIncludesExcludesResponse;
import com.tourya.api.repository.TourIncludesExcludesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourIncludesExcludesService {

    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final ProviderService providerService;
    private final TourService tourService;
    private final TourIncludesExcludesMapper tourIncludesExcludesMapper;

    public List<TourIncludesExcludesResponse> create(List<TourIncludesExcludesRequest> requests,
                                                     Integer tourId,
                                                     Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = getProvider(user);
        Tour tour = getTour(tourId, provider.getId());

        List<TourIncludesExcludes> includesExcludesList = requests.stream()
                .map(req -> {
                    TourIncludesExcludes item = tourIncludesExcludesMapper.toTourIncludesExcludes(req);
                    item.setTour(tour);
                    return item;
                })
                .collect(Collectors.toList());

        return tourIncludesExcludesRepository.saveAll(includesExcludesList).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .collect(Collectors.toList());
    }

    public List<TourIncludesExcludesResponse> getAllByTour(Integer tourId) {
        return tourIncludesExcludesRepository.findByTourId(tourId).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .collect(Collectors.toList());
    }



    @Transactional
    public List<TourIncludesExcludesResponse> replaceAllForTour(List<TourIncludesExcludesRequest> requests,
                                                                Integer tourId,
                                                                Authentication auth) {

        User user = (User) auth.getPrincipal();

        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("No tienes privilegios para esta operación.");
        }

        Provider provider = getProvider(user);
        Tour tour = getTour(tourId, provider.getId());


        tourIncludesExcludesRepository.deleteByTourId(tourId);


        List<TourIncludesExcludes> nuevos = requests.stream()
                .map(req -> {
                    TourIncludesExcludes entity = tourIncludesExcludesMapper.toTourIncludesExcludes(req);
                    entity.setTour(tour);
                    return entity;
                })
                .collect(Collectors.toList());

        return tourIncludesExcludesRepository.saveAll(nuevos).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .collect(Collectors.toList());
    }

    private Tour getTour(Integer tourId, Integer providerId) {
        Tour tour = tourService.getTourByIdAndProviderId(tourId, providerId);
        if (tour != null) {
            return tour;
        } else {
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }

    private Provider getProvider(User user) {
        Provider provider = providerService.findByUser(user);
        if (provider != null) {
            validateRules(provider);
            return provider;
        } else {
            throw new ResourceNotFoundException("No provider was found assigning this user.");
        }
    }

    private void validateRules(Provider provider) {
        if (!provider.getStatus().equals(ProviderStatusEnum.ACTIVE)) {
            throw new OperationNotPermittedException("The provider cannot modify tour data because it is not active.");
        }
    }
}


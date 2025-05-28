package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
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
                                                     Integer tourId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTour(tourId, provider.getId());

        List<TourIncludesExcludes> includesExcludesList = requests.stream()
                .map(req -> {
                    TourIncludesExcludes item = tourIncludesExcludesMapper.toTourIncludesExcludes(req);
                    item.setTour(tour);
                    return item;
                })
                .toList();

        return tourIncludesExcludesRepository.saveAll(includesExcludesList).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .toList();
    }

    public List<TourIncludesExcludesResponse> getAllByTour(Integer tourId, IncludeExcludeTypeEnum type) {
        if(type != null){
            return tourIncludesExcludesRepository.findByTourIdAndType(tourId, type).stream()
                    .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                    .toList();
        }else{
            return tourIncludesExcludesRepository.findByTourId(tourId).stream()
                    .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                    .toList();
        }
    }



    @Transactional
    public List<TourIncludesExcludesResponse> replaceAllForTour(List<TourIncludesExcludesRequest> requests,
                                                                Integer tourId, IncludeExcludeTypeEnum type,
                                                                Authentication auth) {

        User user = (User) auth.getPrincipal();

        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTour(tourId, provider.getId());

        List<TourIncludesExcludes>  list = tourIncludesExcludesRepository.findByTourIdAndType(tourId, type);
        tourIncludesExcludesRepository.deleteAll(list);

        List<TourIncludesExcludes> newList = requests.stream()
                .map(req -> {
                    TourIncludesExcludes entity = tourIncludesExcludesMapper.toTourIncludesExcludes(req);
                    entity.setTour(tour);
                    return entity;
                })
                .toList();

        tourIncludesExcludesRepository.saveAll(newList);
        return tourIncludesExcludesRepository.findByTourIdAndType(tourId, type).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .toList();
    }

    private Tour getTour(Integer tourId, Integer providerId) {
        Tour tour = tourService.getTourByIdAndProviderId(tourId, providerId);
        if (tour != null) {
            return tour;
        } else {
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }
}


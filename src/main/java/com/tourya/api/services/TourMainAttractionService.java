package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.TourMainAttractionMapper;
import com.tourya.api.models.request.TourMainAttractionRequest;
import com.tourya.api.models.responses.TourMainAttractionResponse;
import com.tourya.api.repository.TourMainAttractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class TourMainAttractionService {

    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final ProviderService providerService;
    private final TourService tourService;
    private final TourMainAttractionMapper tourMainAttractionMapper;

    public List<TourMainAttractionResponse> create(List<TourMainAttractionRequest> requests,
                                                   Integer tourId, Authentication connectedUser) {

        User user = ((User) connectedUser.getPrincipal());

        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTour(tourId, provider.getId());

        List<TourMainAttraction> tourMainAttractionList = requests.stream()
                .map(req -> {
                    TourMainAttraction item = tourMainAttractionMapper.toTourMainAttraction(req);
                    item.setTour(tour);
                    return item;
                })
                .toList();

        return tourMainAttractionRepository.saveAll(tourMainAttractionList)
                .stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .toList();
    }

    public List<TourMainAttractionResponse> getAllByTour(Integer tourId) {
        return tourMainAttractionRepository.findByTourId(tourId)
                .stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .toList();
    }

    @Transactional
    public List<TourMainAttractionResponse> replaceAllForTour(List<TourMainAttractionRequest> requests,
                                                              Integer tourId,
                                                              Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = providerService.findByUserAndStatusActive(user);
        Tour tour = getTour(tourId, provider.getId());


        tourMainAttractionRepository.deleteByTourId(tourId);

        List<TourMainAttraction> newAttractions = requests.stream()
                .map(req -> {
                    TourMainAttraction entity = tourMainAttractionMapper.toTourMainAttraction(req);
                    entity.setTour(tour);
                    return entity;
                })
                .toList();

        return tourMainAttractionRepository.saveAll(newAttractions).stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .toList();
    }

    private Tour getTour(Integer tourId, Integer providerId){
        Tour tour =  tourService.getTourByIdAndProviderId(tourId, providerId);
        if(tour != null){
            return tour;
        }else{
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }
}
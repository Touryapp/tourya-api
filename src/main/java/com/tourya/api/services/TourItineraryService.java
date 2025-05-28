package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.TourItineraryMapper;
import com.tourya.api.models.responses.TourItineraryResponse;
import com.tourya.api.models.resquest.TourItineraryRequest;
import com.tourya.api.repository.TourItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourItineraryService {

    private final TourItineraryRepository tourItineraryRepository;
    private final ProviderService providerService;
    private final TourService tourService;
    private final TourItineraryMapper tourItineraryMapper;

    public List<TourItineraryResponse> create(List<TourItineraryRequest> requests,
                                                   Integer tourId, Authentication connectedUser) {

        User user = ((User) connectedUser.getPrincipal());

        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
        Provider provider = getProvider(user);
        Tour tour = getTour(tourId, provider.getId());

        List<TourItinerary> TourItineraryList = requests.stream()
                .map(req -> {
                    TourItinerary item = tourItineraryMapper.toTourItinerary(req);
                    item.setTour(tour);
                    return item;
                })
                .collect(Collectors.toList());

        return tourItineraryRepository.saveAll(TourItineraryList)
                .stream()
                .map(tourItineraryMapper::toTourItineraryResponse)
                .collect(Collectors.toList());
    }

    public List<TourItineraryResponse> getAllByTour(Integer tourId) {
        return tourItineraryRepository.findByTourId(tourId)
                .stream()
                .map(tourItineraryMapper::toTourItineraryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TourItineraryResponse> replaceAllForTour(List<TourItineraryRequest> requests,
                                                              Integer tourId,
                                                              Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        if (!Utils.isProvider(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Provider provider = getProvider(user);
        Tour tour = getTour(tourId, provider.getId());


        tourItineraryRepository.deleteByTourId(tourId);

        List<TourItinerary> newAttractions = requests.stream()
                .map(req -> {
                    TourItinerary entity = tourItineraryMapper.toTourItinerary(req);
                    entity.setTour(tour);
                    return entity;
                })
                .collect(Collectors.toList());

        return tourItineraryRepository.saveAll(newAttractions).stream()
                .map(tourItineraryMapper::toTourItineraryResponse)
                .collect(Collectors.toList());
    }

    private Tour getTour(Integer tourId, Integer providerId){
        Tour tour =  tourService.getTourByIdAndProviderId(tourId, providerId);
        if(tour != null){
            return tour;
        }else{
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }
    private Provider getProvider(User user){
        Provider provider = providerService.findByUser(user);
        if(provider != null){
            validateRules(provider);
            return provider;
        }else{
            throw new ResourceNotFoundException("No provider was found assigning this user.");
        }
    }
    private void validateRules(Provider provider){
        if(!provider.getStatus().equals(ProviderStatusEnum.ACTIVE)){
            throw new OperationNotPermittedException("The provider cannot create a tour as its status is not active.");
        }
    }
}
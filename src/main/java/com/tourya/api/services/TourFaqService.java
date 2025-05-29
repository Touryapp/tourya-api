package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourFaq;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourFaqMapper;
import com.tourya.api.models.responses.TourFaqResponse;
import com.tourya.api.models.resquest.TourFaqRequest;
import com.tourya.api.repository.TourFaqRepository;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourFaqService {
    private final TourFaqRepository tourFaqRepository;
    private final TourRepository tourRepository;
    private final ProviderService providerService;
    private final TourFaqMapper tourFaqMapper;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";
    public TourFaqResponse saveTourFaqByTourId(TourFaqRequest tourFaqRequest,
                                               Integer tourId, Authentication connectedUser){

        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());

            TourFaq tourFaq = tourFaqMapper.toTourFaq(tourFaqRequest);
            tourFaq.setTour(tour);
            return tourFaqMapper.toTourFaqResponse(tourFaqRepository.save(tourFaq));
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public List<TourFaqResponse> saveTourFaqListByTourId(List<TourFaqRequest> tourFaqRequestList,
                                               Integer tourId, Authentication connectedUser){

        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());

            for(TourFaqRequest tourFaqRequest:tourFaqRequestList){
                TourFaq tourFaq = tourFaqMapper.toTourFaq(tourFaqRequest);
                tourFaq.setTour(tour);
                tourFaqRepository.save(tourFaq);
            }

            return  tourFaqRepository.findByTourId(tour.getId()).stream()
                    .map(tourFaqMapper::toTourFaqResponse)
                    .toList();
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    @Transactional
    public List<TourFaqResponse> replaceAll(List<TourFaqRequest> tourFaqRequestList,
                                                         Integer tourId, Authentication connectedUser){

        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());

            List<TourFaq> tourFaqList = tourFaqRepository.findByTourId(tour.getId());
            tourFaqRepository.deleteAll(tourFaqList);

            for(TourFaqRequest tourFaqRequest:tourFaqRequestList){
                TourFaq tourFaq = tourFaqMapper.toTourFaq(tourFaqRequest);
                tourFaq.setTour(tour);
                tourFaqRepository.save(tourFaq);
            }

            return  tourFaqRepository.findByTourId(tour.getId()).stream()
                    .map(tourFaqMapper::toTourFaqResponse)
                    .toList();
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    private Tour getTour(Integer tourId, Integer providerId){
        Tour tour =  tourRepository.findTourByIdAndProviderId(tourId, providerId);
        if(tour != null){
            return tour;
        }else{
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }
}

package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCancellationPolicy;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourCancellationPolicyMapper;
import com.tourya.api.models.request.TourCancellationPolicyRequest;
import com.tourya.api.models.responses.TourCancellationPolicyResponse;
import com.tourya.api.repository.TourCancellationPolicyRepository;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourCancellationPolicyService {
    private final TourCancellationPolicyRepository tourCancellationPolicyRepository;
    private final TourRepository tourRepository;
    private final ProviderService providerService;
    private final TourCancellationPolicyMapper tourCancellationPolicyMapper;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    public TourCancellationPolicyResponse saveCancellationPolicyByTourId(TourCancellationPolicyRequest tourCancellationPolicyRequest,
                                                                         Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());
            TourCancellationPolicy tourCancellationPolicy = tourCancellationPolicyMapper.toTourCancellationPolicy(tourCancellationPolicyRequest);
            tourCancellationPolicy.setTour(tour);

            return tourCancellationPolicyMapper.toTourCancellationPolicyResponse(tourCancellationPolicyRepository.save(tourCancellationPolicy));
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    @Transactional
    public List<TourCancellationPolicyResponse> replaceAll(List<TourCancellationPolicyRequest> tourCancellationPolicyRequestList,
                                            Integer tourId, Authentication connectedUser){

        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());

            List<TourCancellationPolicy> tourCancellationPolicyList = tourCancellationPolicyRepository.findByTourId(tour.getId());
            tourCancellationPolicyRepository.deleteAll(tourCancellationPolicyList);

            for(TourCancellationPolicyRequest tourCancellationPolicyRequest:tourCancellationPolicyRequestList){
                TourCancellationPolicy tourCancellationPolicy = tourCancellationPolicyMapper.toTourCancellationPolicy(tourCancellationPolicyRequest);
                tourCancellationPolicy.setTour(tour);
                tourCancellationPolicyRepository.save(tourCancellationPolicy);
            }

            return  tourCancellationPolicyRepository.findByTourId(tour.getId()).stream()
                    .map(tourCancellationPolicyMapper::toTourCancellationPolicyResponse)
                    .toList();
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public List<TourCancellationPolicyResponse> saveTourCancellationPolicyListByTourId(List<TourCancellationPolicyRequest> tourCancellationPolicyRequestList,
                                                         Integer tourId, Authentication connectedUser){

        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());

            for(TourCancellationPolicyRequest tourCancellationPolicyRequest: tourCancellationPolicyRequestList){
                TourCancellationPolicy tourCancellationPolicy = tourCancellationPolicyMapper.toTourCancellationPolicy(tourCancellationPolicyRequest);
                tourCancellationPolicy.setTour(tour);
                tourCancellationPolicyRepository.save(tourCancellationPolicy);
            }

            return  tourCancellationPolicyRepository.findByTourId(tour.getId()).stream()
                    .map(tourCancellationPolicyMapper::toTourCancellationPolicyResponse)
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

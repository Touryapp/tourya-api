package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCategory;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourMapper;
import com.tourya.api.models.responses.TourAddressResponse;
import com.tourya.api.models.responses.TourDetailsResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.resquest.TourCreateRequest;
import com.tourya.api.models.resquest.TourRequest;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourService {
    private final TourRepository tourRepository;
    private final TourCategoryService tourCategoryService;
    private final TourMapper tourMapper;
    private final ProviderService providerService;
    private final TourAddressService tourAddressService;

    public TourResponse save(TourRequest tourRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = getProvider(user);
            TourCategory tourCategory = getTourCategory(tourRequest.getTourCategoryId());
            Tour tour = tourMapper.toTour(tourRequest);
            tour.setProvider(provider);
            tour.setTourCategory(tourCategory);
            return tourMapper.toTourResponse(tourRepository.save(tour));
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
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
    private TourCategory getTourCategory(Integer id){
        TourCategory tourCategory = tourCategoryService.findById(id);
        if(tourCategory != null){
            return tourCategory;
        }else{
            throw new ResourceNotFoundException("No tourCategory found for the id= " +id);
        }
    }

    public PageResponse<TourResponse> findAllByUser(int page, int size, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Provider provider = getProvider(user);
            Page<Tour> allTours = tourRepository.findAllByProviderId(provider.getId(), pageable);

            List<TourResponse> toursResponse = allTours.stream()
                    .map(tourMapper::toTourResponse)
                    .toList();

            return new PageResponse<>(
                    toursResponse,
                    allTours.getNumber(),
                    allTours.getSize(),
                    allTours.getTotalElements(),
                    allTours.getTotalPages(),
                    allTours.isFirst(),
                    allTours.isLast()
            );

        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    public Tour getTourByIdAndProviderId(Integer id, Integer providerId){
        return tourRepository.findTourByIdAndProviderId(id, providerId);
    }

    public TourDetailsResponse saveCreate(TourCreateRequest tourCreateRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = getProvider(user);
            TourCategory tourCategory = getTourCategory(tourCreateRequest.getTourRequest().getTourCategoryId());
            Tour tour = tourMapper.toTour(tourCreateRequest.getTourRequest());
            tour.setProvider(provider);
            tour.setTourCategory(tourCategory);
            Tour tourNew =  tourRepository.save(tour);

            TourResponse tourResponse = tourMapper.toTourResponse(tourNew);
            TourAddressResponse tourAddressResponse = tourAddressService.saveTourAddressByTourId(tourCreateRequest.getTourAddressRequest(), tourNew.getId(), connectedUser);
            TourDetailsResponse tourDetailsResponse =  new TourDetailsResponse();
            tourDetailsResponse.setTourResponse(tourResponse);
            tourDetailsResponse.setTourAddressResponse(tourAddressResponse);
            return  tourDetailsResponse;
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
}

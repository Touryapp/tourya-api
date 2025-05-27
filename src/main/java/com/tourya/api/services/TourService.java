package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.constans.enums.TourStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.City;
import com.tourya.api.models.Country;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.State;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourAddress;
import com.tourya.api.models.TourCategory;
import com.tourya.api.models.TourIncludesExcludes;
import com.tourya.api.models.TourMainAttraction;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourAddressMapper;
import com.tourya.api.models.mapper.TourIncludesExcludesMapper;
import com.tourya.api.models.mapper.TourMainAttractionMapper;
import com.tourya.api.models.mapper.TourMapper;
import com.tourya.api.models.responses.TourAddressResponse;
import com.tourya.api.models.responses.TourDetailsResponse;
import com.tourya.api.models.responses.TourFullDataResponse;
import com.tourya.api.models.responses.TourIncludesExcludesResponse;
import com.tourya.api.models.responses.TourMainAttractionResponse;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.resquest.TourAddressRequest;
import com.tourya.api.models.resquest.TourCreateRequest;
import com.tourya.api.models.resquest.TourFullDataRequest;
import com.tourya.api.models.resquest.TourIncludesExcludesRequest;
import com.tourya.api.models.resquest.TourMainAttractionRequest;
import com.tourya.api.models.resquest.TourRequest;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourIncludesExcludesRepository;
import com.tourya.api.repository.TourMainAttractionRepository;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService {
    private final TourRepository tourRepository;
    private final TourAddressRepository tourAddressRepository;
    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final TourCategoryService tourCategoryService;
    private final ProviderService providerService;
    private final CountryService countryService;
    private final CityService cityService;
    private final StateService stateService;
    private final TourMapper tourMapper;
    private final TourMainAttractionMapper tourMainAttractionMapper;
    private final TourIncludesExcludesMapper tourIncludesExcludesMapper;
    private final TourAddressMapper tourAddressMapper;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

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
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
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
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }

    public Tour getTourByIdAndProviderId(Integer id, Integer providerId){
        return tourRepository.findTourByIdAndProviderId(id, providerId);
    }
    @Transactional
    public TourDetailsResponse saveCreateBasicData(TourCreateRequest tourCreateRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = getProvider(user);
            TourCategory tourCategory = getTourCategory(tourCreateRequest.getTourCategoryId());
            Tour tour = tourMapper.toTour(tourCreateRequest);
            tour.setProvider(provider);
            tour.setTourCategory(tourCategory);
            tour.setStatus(TourStatusEnum.CREATED);
            Tour tourNew =  tourRepository.save(tour);

            List<TourAddressResponse> tourAddressResponseList = saveTourAddressListByTourId(tourCreateRequest.getLocations(), tourNew);
            TourDetailsResponse tourDetailsResponse =  new TourDetailsResponse();
            tourDetailsResponse.setId(tourNew.getId());
            tourDetailsResponse.setName(tourNew.getName());
            tourDetailsResponse.setDescription(tourNew.getDescription());
            tourDetailsResponse.setDuration(tourNew.getDuration());
            tourDetailsResponse.setMaxPeople(tourNew.getMaxPeople());
            tourDetailsResponse.setHighlight(tourNew.getHighlight());
            tourDetailsResponse.setLocations(tourAddressResponseList);
            return  tourDetailsResponse;
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    private List<TourAddressResponse> saveTourAddressListByTourId(List<TourAddressRequest> tourAddressRequestList,
                                                                  Tour tour){
        List<TourAddress> tourAddressList = new ArrayList<>();
        for(TourAddressRequest tourAddressRequest : tourAddressRequestList){
            TourAddress tourAddress = tourAddressMapper.toTourAddress(tourAddressRequest);
            Country country = getCountry(tourAddressRequest.getCountryId());
            State state = getState(tourAddressRequest.getStateId());
            City city = getCity(tourAddressRequest.getCityId());
            tourAddress.setTour(tour);
            tourAddress.setCountry(country);
            tourAddress.setState(state);
            tourAddress.setCity(city);
            tourAddressList.add(tourAddress);
        }
        return tourAddressRepository.saveAll(tourAddressList).stream()
                    .map(tourAddressMapper::toTourAddressResponse)
                    .toList();

    }
    private State getState(Integer stateId){
        State state = stateService.findById(stateId);
        if(state != null){
            return state;
        }else{
            throw new ResourceNotFoundException("No state found with the id = "+ stateId);
        }
    }

    private Country getCountry(Integer countryId){
        Country country = countryService.findById(countryId);
        if(country != null){
            return country;
        }else{
            throw new ResourceNotFoundException("No country found with the id = "+ countryId);
        }
    }

    private City getCity(Integer cityId){
        City city = cityService.findById(cityId);
        if(city != null){
            return city;
        }else{
            throw new ResourceNotFoundException("No city found with the id = "+ cityId);
        }
    }

    @Transactional
    public TourFullDataResponse saveCreateFullData(TourFullDataRequest tourFullDataRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = getProvider(user);
            TourCategory tourCategory = getTourCategory(tourFullDataRequest.getTourCategoryId());
            //Save tour
            Tour tour = tourMapper.toTour(tourFullDataRequest);
            tour.setProvider(provider);
            tour.setTourCategory(tourCategory);
            tour.setStatus(TourStatusEnum.CREATED);
            Tour tourNew =  tourRepository.save(tour);

            List<TourAddressResponse> tourAddressResponseList = saveTourAddressListByTourId(tourFullDataRequest.getLocations(), tourNew);

            List<TourMainAttractionRequest> tourMainAttractionRequestList = tourFullDataRequest.getMainAttractions();
            List<TourMainAttractionResponse>  tourMainAttractionResponseList  =  mainAttractionReplaceAllForTour(tourMainAttractionRequestList, tourNew);

            List<TourIncludesExcludesRequest> tourIncludesRequest = tourFullDataRequest.getIncludes();
            List<TourIncludesExcludesResponse>  tourIncludesResponseList  =  includesExcludesReplaceAllForTour(tourIncludesRequest, tourNew, IncludeExcludeTypeEnum.INCLUDE);

            List<TourIncludesExcludesRequest> tourExcludesRequest = tourFullDataRequest.getExcludes();
            List<TourIncludesExcludesResponse>  tourExcludesResponseList  =  includesExcludesReplaceAllForTour(tourExcludesRequest, tourNew, IncludeExcludeTypeEnum.EXCLUDE);

            TourFullDataResponse tourFullDataResponse = new TourFullDataResponse();
            tourFullDataResponse.setId(tourNew.getId());
            tourFullDataResponse.setName(tourNew.getName());
            tourFullDataResponse.setDescription(tourNew.getDescription());
            tourFullDataResponse.setDescription(tourNew.getDescription());
            tourFullDataResponse.setTourCategoryId(tourNew.getTourCategory().getId());
            tourFullDataResponse.setDuration(tourNew.getDuration());
            tourFullDataResponse.setMaxPeople(tourNew.getMaxPeople());
            tourFullDataResponse.setHighlight(tourNew.getHighlight());
            tourFullDataResponse.setStatus(tourNew.getStatus());
            tourFullDataResponse.setLocations(tourAddressResponseList);
            tourFullDataResponse.setMainAttractions(tourMainAttractionResponseList);
            tourFullDataResponse.setIncludes(tourIncludesResponseList);
            tourFullDataResponse.setExcludes(tourExcludesResponseList);
            return  tourFullDataResponse;
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    private List<TourMainAttractionResponse> mainAttractionReplaceAllForTour(List<TourMainAttractionRequest> requests,
                                                              Tour tour) {

        tourMainAttractionRepository.deleteByTourId(tour.getId());

        List<TourMainAttraction> newAttractions = requests.stream()
                .map(req -> {
                    TourMainAttraction entity = tourMainAttractionMapper.toTourMainAttraction(req);
                    entity.setTour(tour);
                    return entity;
                })
                .collect(Collectors.toList());

        return tourMainAttractionRepository.saveAll(newAttractions).stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .collect(Collectors.toList());
    }
    private List<TourIncludesExcludesResponse> includesExcludesReplaceAllForTour(List<TourIncludesExcludesRequest> requests,
                                                                Tour tour,
                                                                IncludeExcludeTypeEnum type) {


        List<TourIncludesExcludes>  list = tourIncludesExcludesRepository.findByTourIdAndType(tour.getId(), type);
        tourIncludesExcludesRepository.deleteAll(list);

        List<TourIncludesExcludes> newList = requests.stream()
                .map(req -> {
                    TourIncludesExcludes entity = tourIncludesExcludesMapper.toTourIncludesExcludes(req);
                    entity.setTour(tour);
                    return entity;
                })
                .collect(Collectors.toList());

        tourIncludesExcludesRepository.saveAll(newList);
        return tourIncludesExcludesRepository.findByTourIdAndType(tour.getId(), type).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .collect(Collectors.toList());
    }
}

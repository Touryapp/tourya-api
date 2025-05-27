package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.City;
import com.tourya.api.models.Country;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.State;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourAddress;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourAddressMapper;
import com.tourya.api.models.responses.TourAddressResponse;
import com.tourya.api.models.resquest.TourAddressRequest;
import com.tourya.api.repository.TourAddressRepository;
import com.tourya.api.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourAddressService {
    private final TourAddressRepository tourAddressRepository;
    private final TourAddressMapper tourAddressMapper;
    private final ProviderService providerService;
    private final TourRepository tourRepository;
    private final CountryService countryService;
    private final CityService cityService;
    private final StateService stateService;

    public TourAddressResponse saveTourAddressByTourId(TourAddressRequest tourAddressRequest,
                                                       Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());
            TourAddress tourAddress = tourAddressMapper.toTourAddress(tourAddressRequest);
            Country country = getCountry(tourAddressRequest.getCountryId());
            State state = getState(tourAddressRequest.getStateId());
            City city = getCity(tourAddressRequest.getCityId());


            tourAddress.setTour(tour);
            tourAddress.setCountry(country);
            tourAddress.setState(state);
            tourAddress.setCity(city);
            return tourAddressMapper.toTourAddressResponse(tourAddressRepository.save(tourAddress));
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    public List<TourAddressResponse> saveTourAddressListByTourId(List<TourAddressRequest> tourAddressRequestList,
                                                       Integer tourId, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProvider(roleList)){
            Provider provider = providerService.findByUserAndStatusActive(user);
            Tour tour = getTour(tourId, provider.getId());
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

        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
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
    private Tour getTour(Integer tourId, Integer providerId){
        Tour tour =  tourRepository.findTourByIdAndProviderId(tourId, providerId);
        if(tour != null){
            return tour;
        }else{
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }
}

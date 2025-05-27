package com.tourya.api.models.mapper;

import com.tourya.api.models.TourAddress;
import com.tourya.api.models.responses.TourAddressResponse;
import com.tourya.api.models.resquest.TourAddressRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourAddressMapper {

    private final TourMapper tourMapper;
    public TourAddress toTourAddress(TourAddressRequest tourAddressRequest){
        TourAddress tourAddress = new TourAddress();
        tourAddress.setAddress(tourAddressRequest.getAddress());
        tourAddress.setAddressType(tourAddressRequest.getAddressType());
        tourAddress.setLongitude(tourAddressRequest.getLongitude());
        tourAddress.setLatitude(tourAddressRequest.getLatitude());
        tourAddress.setAddressType(tourAddressRequest.getAddressType());
        return tourAddress;
    }

    public TourAddressResponse toTourAddressResponse(TourAddress tourAddress){
        TourAddressResponse tourAddressResponse = new TourAddressResponse();
        tourAddressResponse.setId(tourAddress.getId());
        tourAddressResponse.setAddress(tourAddress.getAddress());
        tourAddressResponse.setAddressType(tourAddress.getAddressType());
        tourAddressResponse.setLatitude(tourAddress.getLatitude());
        tourAddressResponse.setLongitude(tourAddress.getLongitude());
        tourAddressResponse.setCountryId(tourAddress.getCountry().getId());
        tourAddressResponse.setStateId(tourAddress.getState().getId());
        tourAddressResponse.setCityId(tourAddress.getCity().getId());
        //tourAddressResponse.setTour(tourMapper.toTourResponse(tourAddress.getTour()));
        return tourAddressResponse;
    }
}

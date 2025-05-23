package com.tourya.api.models.mapper;


import com.tourya.api.models.City;
import com.tourya.api.models.responses.CityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityMapper {

    private final StateMapper stateMapper;

    public CityResponse toCityResponse(City city){
        CityResponse cityResponse = new CityResponse();
        cityResponse.setId(city.getId());
        cityResponse.setName(city.getName());
        cityResponse.setState(stateMapper.toStateResponse(city.getState()));
        return cityResponse;
    }
}

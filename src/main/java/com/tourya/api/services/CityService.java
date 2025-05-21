package com.tourya.api.services;

import com.tourya.api.models.City;
import com.tourya.api.models.Country;
import com.tourya.api.models.mapper.CityMapper;
import com.tourya.api.models.responses.CityResponse;
import com.tourya.api.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CityService {
    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    public List<CityResponse> getAllCityByStateIdList(Integer stateId){
        List<City> allCity = cityRepository.getAllCityByStateIdList(stateId);

        return allCity.stream()
                .map(cityMapper::toCityResponse)
                .toList();
    }

    public City findById(Integer id){
        Optional<City> optionalCity = cityRepository.findById(id);
        if(optionalCity.isPresent()){
            return  optionalCity.get();
        }else{
            return null;
        }
    }
}

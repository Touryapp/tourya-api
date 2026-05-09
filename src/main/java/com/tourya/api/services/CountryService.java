package com.tourya.api.services;


import com.tourya.api.models.Country;
import com.tourya.api.models.mapper.CountryMapper;
import com.tourya.api.models.responses.CountryResponse;
import com.tourya.api.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CountryService {
    private final CountryRepository countryRepository;
    private final CountryMapper countryMapper;

    public List<CountryResponse> getAllCountryList(){
        List<Country> allCountry = countryRepository.findAll();

        return allCountry.stream()
                .map(countryMapper::toCountryResponse)
                .toList();

    }

    public Country findById(Integer id){
        Optional<Country>  optionalCountry = countryRepository.findById(id);
        if(optionalCountry.isPresent()){
            return  optionalCountry.get();
        }else{
            return null;
        }
    }
}

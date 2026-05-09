package com.tourya.api.models.mapper;

import com.tourya.api.models.Country;
import com.tourya.api.models.responses.CountryResponse;
import org.springframework.stereotype.Service;

@Service
public class CountryMapper {

    public CountryResponse toCountryResponse(Country country){
        CountryResponse countryResponse = new CountryResponse();
        countryResponse.setId(country.getId());
        countryResponse.setName(country.getName());
        return countryResponse;
    }
}

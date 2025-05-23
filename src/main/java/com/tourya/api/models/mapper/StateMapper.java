package com.tourya.api.models.mapper;

import com.tourya.api.models.State;
import com.tourya.api.models.responses.StateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StateMapper {

    private final CountryMapper countryMapper;

    public StateResponse toStateResponse(State state){
        StateResponse stateResponse = new StateResponse();
        stateResponse.setId(state.getId());
        stateResponse.setName(state.getName());
        stateResponse.setCountry(countryMapper.toCountryResponse(state.getCountry()));
        return stateResponse;
    }
}

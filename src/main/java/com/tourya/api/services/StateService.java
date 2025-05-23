package com.tourya.api.services;

import com.tourya.api.models.City;
import com.tourya.api.models.State;
import com.tourya.api.models.mapper.StateMapper;
import com.tourya.api.models.responses.StateResponse;
import com.tourya.api.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StateService {
    private final StateRepository stateRepository;
    private final StateMapper stateMapper;

    public List<StateResponse> getAllStateByCountryIdList(Integer countryId){
        List<State> allState = stateRepository.getAllStateByCountryId(countryId);

        return allState.stream()
                .map(stateMapper::toStateResponse)
                .toList();
    }

    public State findById(Integer id){
        Optional<State> optionalState = stateRepository.findById(id);
        if(optionalState.isPresent()){
            return  optionalState.get();
        }else{
            return null;
        }
    }

}

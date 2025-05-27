package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ProviderStatusEnum;
import com.tourya.api.constans.enums.RequestProviderStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.City;
import com.tourya.api.models.Country;
import com.tourya.api.models.Provider;
import com.tourya.api.models.Role;
import com.tourya.api.models.RequestProvider;
import com.tourya.api.models.State;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.ProviderMapper;
import com.tourya.api.models.responses.ProviderResponse;
import com.tourya.api.models.resquest.ProviderRequest;
import com.tourya.api.repository.ProviderRepository;
import com.tourya.api.repository.RequestProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProviderService {
    private final ProviderRepository providerRepository;
    private final ProviderMapper providerMapper;
    private final RequestProviderRepository requestProviderRepository;
    private final CountryService countryService;
    private final CityService cityService;
    private final StateService stateService;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    public Provider save(Provider provider) {
        return providerRepository.save(provider);
    }

    public ProviderResponse update(ProviderRequest providerRequest,
                                   Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        Provider provider = providerRepository.findByUser(user);
        if(provider != null){
            provider.setName(providerRequest.getName());
            provider.setDocumentNumber(providerRequest.getDocumentNumber());
            provider.setDocumentType(providerRequest.getDocumentType());
            provider.setServiceType(providerRequest.getServiceType());
            provider.setCountry(getCountry(providerRequest.getCountryId()));
            provider.setCity(getCity(providerRequest.getCityId()));
            provider.setState(getState(providerRequest.getStateId()));
            provider.setDepartment(providerRequest.getDepartment());
            provider.setAddress(providerRequest.getAddress());
            provider.setPhone(providerRequest.getPhone());
            return providerMapper.toProviderResponse(providerRepository.save(provider));
        }else{
            throw new ResourceNotFoundException("Provider not found for user: "+ user.getEmail());
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
    private State getState(Integer stateId){
        State state = stateService.findById(stateId);
        if(state != null){
            return state;
        }else{
            throw new ResourceNotFoundException("No state found with the id = "+ stateId);
        }
    }
    public ProviderResponse consultDataProvider(Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        Provider provider = providerRepository.findByUser(user);
        if(provider != null){
            return providerMapper.toProviderResponse(provider);
        }else{
            throw new ResourceNotFoundException("Provider not found for user: "+ user.getEmail());
        }
    }

    public Provider findByUser(User user) {
        return providerRepository.findByUser(user);
    }

    public Provider findByUserAndStatusActive(User user) {
        Provider provider = providerRepository.findByUser(user);
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
    public Provider findById(Integer providerId) {
        Optional<Provider>  optionalProvider = providerRepository.findById(providerId);
        if(optionalProvider.isPresent()){
            return  optionalProvider.get();
        }else{
            return null;
        }
    }

    public PageResponse<ProviderResponse> findAll(int page, int size, ProviderStatusEnum status, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<Provider> allProviders = providerRepository.findAllProvider(status, pageable);
            List<ProviderResponse> providersResponse = allProviders.stream()
                    .map(providerMapper::toProviderResponse)
                    .toList();

            return new PageResponse<>(
                    providersResponse,
                    allProviders.getNumber(),
                    allProviders.getSize(),
                    allProviders.getTotalElements(),
                    allProviders.getTotalPages(),
                    allProviders.isFirst(),
                    allProviders.isLast()
            );
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public ProviderResponse consultDataProviderById(Integer providerId, Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if (Utils.isAdmin(roleList)) {
            Provider provider = this.findById(providerId);
            if(provider != null){
                return providerMapper.toProviderResponse(provider);
            }else{
                throw new ResourceNotFoundException("Provider not found. Id:"+providerId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public void deleteProviderById(Integer providerId, Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if (Utils.isAdmin(roleList)) {
            Provider provider = findById(providerId);
            if (provider != null) {
                RequestProvider requestProvider = this.getRequestProviderByProvider(provider);
                if (requestProvider == null) {
                    providerRepository.delete(provider);
                } else {
                    throw new OperationNotPermittedException("Unable to delete provider, an associated requestProvider was found.");
                }
            } else {
                throw new ResourceNotFoundException("Provider not found. id = " + providerId);
            }
        } else {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    @Transactional
    public ProviderResponse activeOrInactiveProviderById(Integer providerId, ProviderStatusEnum status, Authentication connectedUser ) {
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if (Utils.isAdmin(roleList)) {
            Provider provider = findById(providerId);
            if (provider != null) {
                provider.setStatus(status);
                Provider providerUpdate = providerRepository.save(provider);
                RequestProvider requestProvider = this.getRequestProviderByProvider(provider);
                if (requestProvider != null) {
                    requestProvider.setProvider(providerUpdate);
                    requestProvider.setStatus(status.equals(ProviderStatusEnum.ACTIVE) ?
                            RequestProviderStatusEnum.COMPLETED : RequestProviderStatusEnum.DECLINED);
                    requestProviderRepository.save(requestProvider);
                    return providerMapper.toProviderResponse(providerUpdate);
                } else {
                    throw new OperationNotPermittedException("No requestProvider found associated with the provider.");
                }
            } else {
                throw new ResourceNotFoundException("Provider not found. id = " + providerId);
            }
        } else {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public RequestProvider getRequestProviderByProvider(Provider provider){
        return requestProviderRepository.findByProvider(provider);
    }
}

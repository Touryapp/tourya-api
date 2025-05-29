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
import com.tourya.api.models.mapper.RequestProviderMapper;
import com.tourya.api.models.responses.RequestProviderResponse;
import com.tourya.api.models.request.RequestProviderRequest;
import com.tourya.api.repository.RoleRepository;
import com.tourya.api.repository.RequestProviderRepository;
import com.tourya.api.repository.UserRepository;
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
public class RequestProviderService {
    private final RequestProviderRepository requestProviderRepository;
    private final ProviderService providerService;
    private final ProviderMapper providerMapper;
    private final RequestProviderMapper requestProviderMapper;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CountryService countryService;
    private final CityService cityService;
    private final StateService stateService;
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";
    private static final String REQUEST_PROVIDER_NOT_FOUND = "RequestProvider not found Id: ";
    @Transactional
    public RequestProviderResponse save(RequestProviderRequest request,
                                        Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        Provider providerCurrent = providerService.findByUser(user);
        if(providerCurrent != null && requestProviderRepository.findByProvider(providerCurrent) != null){
            throw new OperationNotPermittedException("It is not possible to create another request, this user already has a provider and request assigned to him.");
        }

        List<Role> roleList =  user.getRoles();

        var userRole = roleRepository.findByName("PROVIDER")
                // todo - better exception handling
                .orElseThrow(() -> new IllegalStateException("ROLE PROVIDER was not initiated"));
        roleList.add(userRole);
        user.setRoles(roleList);
        User userUpdate = userRepository.save(user);


        Provider provider = providerMapper.toProvider(request);
        provider.setCountry(getCountry(request.getCountryId()));
        provider.setCity(getCity(request.getCityId()));
        provider.setState(getState(request.getStateId()));
        provider.setUser(userUpdate);
        provider.setStatus(ProviderStatusEnum.POTENTIAL);
        Provider providerNew = providerService.save(provider);

        RequestProvider requestProvider = new RequestProvider();
        requestProvider.setProvider(providerNew);
        requestProvider.setStatus(RequestProviderStatusEnum.PENDING);

        RequestProvider requestProviderNew = requestProviderRepository.save(requestProvider);

        return requestProviderMapper.toRequestProviderResponse(requestProviderNew);
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

    public RequestProviderResponse consultData(Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());

        Provider provider = providerService.findByUser(user);
        if(provider != null){
            RequestProvider requestProvider = requestProviderRepository.findByProvider(provider);
            return requestProviderMapper.toRequestProviderResponse(requestProvider);
        }else{
            return null;
        }
    }
    public RequestProviderResponse consultDataById(Integer requestProviderId,
                                                   Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<RequestProvider> requestProviderOpt = requestProviderRepository.findById(requestProviderId);
            if(requestProviderOpt.isPresent()){
                RequestProvider requestProvider = requestProviderOpt.get();
                return requestProviderMapper.toRequestProviderResponse(requestProvider);
            }else{
                throw new ResourceNotFoundException(REQUEST_PROVIDER_NOT_FOUND+requestProviderId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    @Transactional
    public RequestProviderResponse approveRequestProviderById(Integer requestProviderId,
                                                        Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<RequestProvider> requestProviderOpt = requestProviderRepository.findById(requestProviderId);
            if(requestProviderOpt.isPresent()){
                RequestProvider requestProvider = requestProviderOpt.get();

                Provider provider = requestProvider.getProvider();
                provider.setStatus(ProviderStatusEnum.ACTIVE);
                providerService.save(provider);

                requestProvider.setStatus(RequestProviderStatusEnum.APPROVED);
                RequestProvider requestProviderUpdate = requestProviderRepository.save(requestProvider);

                return requestProviderMapper.toRequestProviderResponse(requestProviderUpdate);
            }else{
                throw new ResourceNotFoundException(REQUEST_PROVIDER_NOT_FOUND+requestProviderId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    @Transactional
    public RequestProviderResponse declineRequestProviderById(Integer requestProviderId,
                                                        Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Optional<RequestProvider> requestProviderOpt = requestProviderRepository.findById(requestProviderId);
            if(requestProviderOpt.isPresent()){
                RequestProvider requestProvider = requestProviderOpt.get();

                Provider provider = requestProvider.getProvider();
                provider.setStatus(ProviderStatusEnum.INACTIVE);
                providerService.save(provider);

                requestProvider.setStatus(RequestProviderStatusEnum.DECLINED);
                RequestProvider requestProviderUpdate = requestProviderRepository.save(requestProvider);

                return requestProviderMapper.toRequestProviderResponse(requestProviderUpdate);
            }else{
                throw new ResourceNotFoundException(REQUEST_PROVIDER_NOT_FOUND+requestProviderId);
            }
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public PageResponse<RequestProviderResponse> findAll(int page, int size, RequestProviderStatusEnum status, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<RequestProvider> allRequestProviderPending = requestProviderRepository.findAllRequestProviderPending(status, pageable);

            List<RequestProviderResponse> requestProvidersResponse = allRequestProviderPending.stream()
                    .map(requestProviderMapper::toRequestProviderResponse)
                    .toList();
            return new PageResponse<>(
                    requestProvidersResponse,
                    allRequestProviderPending.getNumber(),
                    allRequestProviderPending.getSize(),
                    allRequestProviderPending.getTotalElements(),
                    allRequestProviderPending.getTotalPages(),
                    allRequestProviderPending.isFirst(),
                    allRequestProviderPending.isLast()
            );
        }else{
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }
    }
    public RequestProvider getRequestProviderByProvider(Provider provider){
        return requestProviderRepository.findByProvider(provider);
    }

}

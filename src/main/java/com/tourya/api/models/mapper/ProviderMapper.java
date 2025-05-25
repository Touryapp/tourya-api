package com.tourya.api.models.mapper;


import com.tourya.api.models.Provider;
import com.tourya.api.models.responses.ProviderResponse;
import com.tourya.api.models.resquest.RequestProviderRequest;
import org.springframework.stereotype.Service;


@Service
public class ProviderMapper {

    public Provider toProvider(RequestProviderRequest  request) {
        Provider provider = new Provider();
        provider.setName(request.getName());
        provider.setDocumentNumber(request.getDocumentNumber());
        provider.setDocumentType(request.getDocumentType());
        provider.setServiceType(request.getServiceType());
        //provider.setCiudad(request.getCiudad());
        //provider.setPais(request.getPais());
        provider.setAddress(request.getAddress());
        provider.setPhone(request.getPhone());
        provider.setDepartment(request.getDepartment());
        return provider;
    }

    public ProviderResponse toProviderResponse(Provider provider){
        ProviderResponse providerResponse = new ProviderResponse();
        providerResponse.setId(provider.getId());
        providerResponse.setName(provider.getName());
        providerResponse.setDocumentNumber(provider.getDocumentNumber());
        providerResponse.setDocumentType(provider.getDocumentType());
        providerResponse.setServiceType(provider.getServiceType());
        //providerResponse.setCountry(provider.getCountry());
        //providerResponse.setCity(provider.getCity());
        //providerResponse.setState(provider.getState());
        providerResponse.setDepartment(provider.getDepartment());
        providerResponse.setAddress(provider.getAddress());
        providerResponse.setPhone(provider.getPhone());
        providerResponse.setStatus(provider.getStatus());

        return  providerResponse;
    }

}

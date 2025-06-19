package com.tourya.api.models.mapper;

import com.tourya.api.models.RequestProviderDocumentType;
import com.tourya.api.models.request.RequestProviderDocumentTypeRequest;
import com.tourya.api.models.responses.RequestProviderDocumentTypeResponse;
import org.springframework.stereotype.Service;

@Service
public class RequestProviderDocumentTypeMapper {

    public RequestProviderDocumentType toRequestProviderDocumentType(RequestProviderDocumentTypeRequest requestProviderDocumentTypeRequest){
        RequestProviderDocumentType requestProviderDocumentType = new RequestProviderDocumentType();
        requestProviderDocumentType.setName(requestProviderDocumentTypeRequest.getName());
        requestProviderDocumentType.setDescription(requestProviderDocumentTypeRequest.getDescription());
        requestProviderDocumentType.setMandatory(requestProviderDocumentTypeRequest.getMandatory());
        return requestProviderDocumentType;
    }

    public RequestProviderDocumentTypeResponse toRequestProviderDocumentTypeResponse(RequestProviderDocumentType requestProviderDocumentType){
        RequestProviderDocumentTypeResponse requestProviderDocumentTypeResponse = new RequestProviderDocumentTypeResponse();
        requestProviderDocumentTypeResponse.setId(requestProviderDocumentType.getId());
        requestProviderDocumentTypeResponse.setName(requestProviderDocumentType.getName());
        requestProviderDocumentTypeResponse.setDescription(requestProviderDocumentType.getDescription());
        requestProviderDocumentTypeResponse.setMandatory(requestProviderDocumentType.getMandatory());
        return requestProviderDocumentTypeResponse;
    }
}

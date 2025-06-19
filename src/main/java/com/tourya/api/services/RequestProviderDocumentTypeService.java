package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.models.RequestProviderDocumentType;
import com.tourya.api.models.Role;
import com.tourya.api.models.TourCancelCategory;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.RequestProviderDocumentTypeMapper;
import com.tourya.api.models.request.RequestProviderDocumentTypeRequest;
import com.tourya.api.models.responses.RequestProviderDocumentTypeResponse;
import com.tourya.api.models.responses.TourCancelCategoryResponse;
import com.tourya.api.repository.RequestProviderDocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestProviderDocumentTypeService {
    private final RequestProviderDocumentTypeRepository requestProviderDocumentTypeRepository;
    private final RequestProviderDocumentTypeMapper requestProviderDocumentTypeMapper;

    public RequestProviderDocumentTypeResponse save(RequestProviderDocumentTypeRequest requestProviderDocumentTypeRequest,
                                                    Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            RequestProviderDocumentType requestProviderDocumentType = requestProviderDocumentTypeMapper.toRequestProviderDocumentType(requestProviderDocumentTypeRequest);
            return requestProviderDocumentTypeMapper.toRequestProviderDocumentTypeResponse(requestProviderDocumentTypeRepository.save(requestProviderDocumentType));
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    public PageResponse<RequestProviderDocumentTypeResponse> findAll(int page, int size, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<RequestProviderDocumentType> allRequestProviderDocumentType = requestProviderDocumentTypeRepository.findAll(pageable);
            List<RequestProviderDocumentTypeResponse> requestProviderDocumentTypeResponseList = allRequestProviderDocumentType.stream()
                    .map(requestProviderDocumentTypeMapper::toRequestProviderDocumentTypeResponse)
                    .toList();

            return new PageResponse<>(
                    requestProviderDocumentTypeResponseList,
                    allRequestProviderDocumentType.getNumber(),
                    allRequestProviderDocumentType.getSize(),
                    allRequestProviderDocumentType.getTotalElements(),
                    allRequestProviderDocumentType.getTotalPages(),
                    allRequestProviderDocumentType.isFirst(),
                    allRequestProviderDocumentType.isLast()
            );
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    public List<RequestProviderDocumentTypeResponse> getAllRequestProviderDocumentTypeList(){
        List<RequestProviderDocumentType> allRequestProviderDocumentTypeList = requestProviderDocumentTypeRepository.getAllRequestProviderDocumentTypeList();
        return  allRequestProviderDocumentTypeList.stream()
                .map(requestProviderDocumentTypeMapper::toRequestProviderDocumentTypeResponse)
                .toList();
    }
}

package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.models.Role;
import com.tourya.api.models.TourCancelCategory;
import com.tourya.api.models.TourCategory;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourCancelCategoryMapper;
import com.tourya.api.models.request.TourCancelCategoryRequest;
import com.tourya.api.models.responses.TourCancelCategoryResponse;
import com.tourya.api.repository.TourCancelCategoryRepository;
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
public class TourCancelCategoryService {
    private final TourCancelCategoryRepository tourCancelCategoryRepository;
    private final TourCancelCategoryMapper tourCancelCategoryMapper;
    public TourCancelCategoryResponse save(TourCancelCategoryRequest tourCancelCategoryRequest,
    Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            if(tourCancelCategoryRepository.findByName(tourCancelCategoryRequest.getName()) == null){
                TourCancelCategory tourCancelCategory = tourCancelCategoryMapper.toTourCancelCategory(tourCancelCategoryRequest);
                return tourCancelCategoryMapper.toTourCancelCategoryResponse(tourCancelCategoryRepository.save(tourCancelCategory));
            }else{
                throw new OperationNotPermittedException("Name in use for tourCancelCategory.");
            }
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    public PageResponse<TourCancelCategoryResponse> findAll(int page, int size, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<TourCancelCategory> allTourCancelCategory = tourCancelCategoryRepository.findAll(pageable);
            List<TourCancelCategoryResponse> tourCancelCategoryResponseList = allTourCancelCategory.stream()
                    .map(tourCancelCategoryMapper::toTourCancelCategoryResponse)
                    .toList();

            return new PageResponse<>(
                    tourCancelCategoryResponseList,
                    allTourCancelCategory.getNumber(),
                    allTourCancelCategory.getSize(),
                    allTourCancelCategory.getTotalElements(),
                    allTourCancelCategory.getTotalPages(),
                    allTourCancelCategory.isFirst(),
                    allTourCancelCategory.isLast()
            );
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }
    public List<TourCancelCategoryResponse> getAllTourCancelCategoryList(){
        List<TourCancelCategory> allTourCancelCategoryList = tourCancelCategoryRepository.getAllTourCancelCategoryList();
        return  allTourCancelCategoryList.stream()
                .map(tourCancelCategoryMapper::toTourCancelCategoryResponse)
                .toList();
    }
}

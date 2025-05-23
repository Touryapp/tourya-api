package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.models.Role;
import com.tourya.api.models.TourCategory;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourCategoryMapper;
import com.tourya.api.models.responses.TourCategoryResponse;
import com.tourya.api.models.resquest.TourCategoryRequest;
import com.tourya.api.repository.TourCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TourCategoryService {
    private final TourCategoryRepository tourCategoryRepository;
    private final TourCategoryMapper tourCategoryMapper;

    public TourCategoryResponse save(TourCategoryRequest tourCategoryRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            TourCategory tourCategory = tourCategoryMapper.toTourCategory(tourCategoryRequest);
            return tourCategoryMapper.toTourCategoryResponse(tourCategoryRepository.save(tourCategory));
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    public PageResponse<TourCategoryResponse> findAll(int page, int size, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isAdmin(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Page<TourCategory> allTourCategory = tourCategoryRepository.findAll(pageable);

            List<TourCategoryResponse> toursCategoryResponse = allTourCategory.stream()
                    .map(tourCategoryMapper::toTourCategoryResponse)
                    .toList();

            return new PageResponse<>(
                    toursCategoryResponse,
                    allTourCategory.getNumber(),
                    allTourCategory.getSize(),
                    allTourCategory.getTotalElements(),
                    allTourCategory.getTotalPages(),
                    allTourCategory.isFirst(),
                    allTourCategory.isLast()
            );

        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    public List<TourCategoryResponse> getTourCategoryList(){
        List<TourCategory> allTourCategoryList = tourCategoryRepository.getAllTourCategoryList();
        return  allTourCategoryList.stream()
                .map(tourCategoryMapper::toTourCategoryResponse)
                .toList();
    }

    public TourCategory findById(Integer id){
        Optional<TourCategory> optionalTourCategory = tourCategoryRepository.findById(id);
        if(optionalTourCategory.isPresent()){
            return optionalTourCategory.get();
        }else{
            return null;
        }
    }
}

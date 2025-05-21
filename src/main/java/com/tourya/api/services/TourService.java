package com.tourya.api.services;


import com.tourya.api._utils.Utils;
import com.tourya.api.common.PageResponse;
import com.tourya.api.constans.enums.ProveedorStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Proveedor;
import com.tourya.api.models.Role;
import com.tourya.api.models.Tour;
import com.tourya.api.models.TourCategory;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.TourMapper;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.resquest.TourRequest;
import com.tourya.api.repository.TourRepository;
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
public class TourService {
    private final TourRepository tourRepository;
    private final TourCategoryService tourCategoryService;
    private final TourMapper tourMapper;
    private final ProveedorService proveedorService;

    public TourResponse save(TourRequest tourRequest, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProveedor(roleList)){
            Proveedor proveedor = getProveedor(user);
            TourCategory tourCategory = getTourCategory(tourRequest.getTourCategoryId());
            Tour tour = tourMapper.toTour(tourRequest);
            tour.setProveedor(proveedor);
            tour.setTourCategory(tourCategory);
            return tourMapper.toTourResponse(tourRepository.save(tour));
        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    private Proveedor getProveedor(User user){
        Proveedor proveedor = proveedorService.findByUser(user);
        if(proveedor != null){
            validateRules(proveedor);
            return proveedor;
        }else{
            throw new ResourceNotFoundException("No provider was found assigning this user.");
        }
    }
    private void validateRules(Proveedor proveedor){
        if(!proveedor.getStatus().equals(ProveedorStatusEnum.ACTIVO)){
            throw new OperationNotPermittedException("The provider cannot create a tour as its status is not active.");
        }
    }
    private TourCategory getTourCategory(Integer id){
        TourCategory tourCategory = tourCategoryService.findById(id);
        if(tourCategory != null){
            return tourCategory;
        }else{
            throw new ResourceNotFoundException("No tourCategory found for the id= " +id);
        }
    }

    public PageResponse<TourResponse> findAllByUser(int page, int size, Authentication connectedUser){
        User user = ((User) connectedUser.getPrincipal());
        List<Role> roleList = user.getRoles();
        if(Utils.isProveedor(roleList)){
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            Proveedor proveedor = getProveedor(user);
            Page<Tour> allTours = tourRepository.findAllByProveedorId(proveedor.getId(), pageable);

            List<TourResponse> toursResponse = allTours.stream()
                    .map(tourMapper::toTourResponse)
                    .toList();

            return new PageResponse<>(
                    toursResponse,
                    allTours.getNumber(),
                    allTours.getSize(),
                    allTours.getTotalElements(),
                    allTours.getTotalPages(),
                    allTours.isFirst(),
                    allTours.isLast()
            );

        }else{
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
    }

    public Tour getTourByIdAndProveedorId(Integer id, Integer proveedorId){
        return tourRepository.findTourByIdAndProveedorId(id, proveedorId);
    }
}

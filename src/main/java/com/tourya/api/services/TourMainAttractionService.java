package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.ProveedorStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.TourMainAttractionMapper;
import com.tourya.api.models.resquest.TourMainAttractionRequest;
import com.tourya.api.models.responses.TourMainAttractionResponse;
import com.tourya.api.repository.TourMainAttractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class TourMainAttractionService {

    private final TourMainAttractionRepository tourMainAttractionRepository;
    private final ProveedorService proveedorService;
    private final TourService tourService;
    private final TourMainAttractionMapper tourMainAttractionMapper;

    public List<TourMainAttractionResponse> create(List<TourMainAttractionRequest> requests,
                                                   Integer tourId, Authentication connectedUser) {

        User user = ((User) connectedUser.getPrincipal());

        if (!Utils.isProveedor(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }
        Proveedor proveedor = getProveedor(user);
        Tour tour = getTour(tourId, proveedor.getId());

        List<TourMainAttraction> tourMainAttractionList = requests.stream()
                .map(req -> {
                    TourMainAttraction item = tourMainAttractionMapper.toTourMainAttraction(req);
                    item.setTour(tour);
                    return item;
                })
                .collect(Collectors.toList());

        return tourMainAttractionRepository.saveAll(tourMainAttractionList)
                .stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .collect(Collectors.toList());
    }

    public List<TourMainAttractionResponse> getAllByTour(Integer tourId) {
        return tourMainAttractionRepository.findByTourId(tourId)
                .stream()
                .map(tourMainAttractionMapper::toTourMainAttractionResponse)
                .collect(Collectors.toList());
    }



    private Tour getTour(Integer tourId, Integer proveedorId){
        Tour tour =  tourService.getTourByIdAndProveedorId(tourId, proveedorId);
        if(tour != null){
            return tour;
        }else{
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
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
}
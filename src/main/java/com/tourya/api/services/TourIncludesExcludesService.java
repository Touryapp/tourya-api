package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.ProveedorStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.mapper.TourIncludesExcludesMapper;
import com.tourya.api.models.resquest.TourIncludesExcludesRequest;
import com.tourya.api.models.responses.TourIncludesExcludesResponse;
import com.tourya.api.repository.TourIncludesExcludesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourIncludesExcludesService {

    private final TourIncludesExcludesRepository tourIncludesExcludesRepository;
    private final ProveedorService proveedorService;
    private final TourService tourService;
    private final TourIncludesExcludesMapper tourIncludesExcludesMapper;

    public List<TourIncludesExcludesResponse> create(List<TourIncludesExcludesRequest> requests,
                                                     Integer tourId,
                                                     Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        if (!Utils.isProveedor(user.getRoles())) {
            throw new InsufficientPrivilegesException("You have no privileges to perform this action.");
        }

        Proveedor proveedor = getProveedor(user);
        Tour tour = getTour(tourId, proveedor.getId());

        List<TourIncludesExcludes> includesExcludesList = requests.stream()
                .map(req -> {
                    TourIncludesExcludes item = tourIncludesExcludesMapper.toTourIncludesExcludes(req);
                    item.setTour(tour);
                    return item;
                })
                .collect(Collectors.toList());

        return tourIncludesExcludesRepository.saveAll(includesExcludesList).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .collect(Collectors.toList());
    }

    public List<TourIncludesExcludesResponse> getAllByTour(Integer tourId) {
        return tourIncludesExcludesRepository.findByTourId(tourId).stream()
                .map(tourIncludesExcludesMapper::tourIncludesExcludesResponse)
                .collect(Collectors.toList());
    }




    private Tour getTour(Integer tourId, Integer proveedorId) {
        Tour tour = tourService.getTourByIdAndProveedorId(tourId, proveedorId);
        if (tour != null) {
            return tour;
        } else {
            throw new ResourceNotFoundException("No tour with this id was found for this provider.");
        }
    }

    private Proveedor getProveedor(User user) {
        Proveedor proveedor = proveedorService.findByUser(user);
        if (proveedor != null) {
            validateRules(proveedor);
            return proveedor;
        } else {
            throw new ResourceNotFoundException("No provider was found assigning this user.");
        }
    }

    private void validateRules(Proveedor proveedor) {
        if (!proveedor.getStatus().equals(ProveedorStatusEnum.ACTIVO)) {
            throw new OperationNotPermittedException("The provider cannot modify tour data because it is not active.");
        }
    }
}


package com.tourya.api.models.mapper;

import com.tourya.api.models.Tour;
import com.tourya.api.models.responses.TourResponse;
import com.tourya.api.models.resquest.TourRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourMapper {
    private final ProveedorMapper proveedorMapper;
    private final TourCategoryMapper tourCategoryMapper;

    public Tour toTour(TourRequest tourRequest){
        Tour tour = new Tour();
        tour.setName(tourRequest.getName());
        tour.setDescription(tourRequest.getDescription());
        tour.setDuration(tourRequest.getDuration());
        tour.setMaxPeople(tourRequest.getMaxPeople());
        tour.setHighlight(tourRequest.getHighlight());
        return tour;
    }

    public TourResponse toTourResponse(Tour tour){
        TourResponse tourResponse = new TourResponse();
        tourResponse.setId(tour.getId());
        tourResponse.setName(tour.getName());
        tourResponse.setDescription(tour.getDescription());
        tourResponse.setDuration(tour.getDuration());
        tourResponse.setMaxPeople(tour.getMaxPeople());
        tourResponse.setHighlight(tour.getHighlight());
        tourResponse.setTourCategory(tourCategoryMapper.toTourCategoryResponse(tour.getTourCategory()));
        tourResponse.setProveedor(proveedorMapper.toProveedorResponse(tour.getProveedor()));
        return tourResponse;
    }
}

package com.tourya.api.models.mapper;

import com.tourya.api.models.TourCategory;
import com.tourya.api.models.responses.TourCategoryResponse;
import com.tourya.api.models.resquest.TourCategoryRequest;
import org.springframework.stereotype.Service;

@Service
public class TourCategoryMapper {

    public TourCategoryResponse toTourCategoryResponse(TourCategory tourCategory){
        TourCategoryResponse tourCategoryResponse = new TourCategoryResponse();
        tourCategoryResponse.setId(tourCategory.getId());
        tourCategoryResponse.setName(tourCategory.getName());
        tourCategoryResponse.setDescription(tourCategory.getDescription());
        return tourCategoryResponse;
    }

    public TourCategory toTourCategory(TourCategoryRequest tourCategoryRequest){
        TourCategory tourCategory = new TourCategory();
        tourCategory.setName(tourCategoryRequest.getName());
        tourCategory.setDescription(tourCategoryRequest.getDescription());
        return tourCategory;
    }
}

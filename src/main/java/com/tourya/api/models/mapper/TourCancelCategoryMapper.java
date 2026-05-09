package com.tourya.api.models.mapper;

import com.tourya.api.models.TourCancelCategory;
import com.tourya.api.models.request.TourCancelCategoryRequest;
import com.tourya.api.models.responses.TourCancelCategoryResponse;
import org.springframework.stereotype.Service;

@Service
public class TourCancelCategoryMapper {

    public TourCancelCategory toTourCancelCategory(TourCancelCategoryRequest tourCancelCategoryRequest){
        TourCancelCategory tourCancelCategory = new TourCancelCategory();
        tourCancelCategory.setName(tourCancelCategoryRequest.getName());
        tourCancelCategory.setDescription(tourCancelCategoryRequest.getDescription());
        return tourCancelCategory;
    }

    public TourCancelCategoryResponse toTourCancelCategoryResponse(TourCancelCategory tourCancelCategory){
        TourCancelCategoryResponse tourCancelCategoryResponse = new TourCancelCategoryResponse();
        tourCancelCategoryResponse.setId(tourCancelCategory.getId());
        tourCancelCategoryResponse.setName(tourCancelCategory.getName());
        tourCancelCategoryResponse.setDescription(tourCancelCategory.getDescription());
        return tourCancelCategoryResponse;
    }
}

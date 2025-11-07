package com.tourya.api.models.mapper;

import com.tourya.api.models.Tour;
import com.tourya.api.models.responses.*;
import com.tourya.api.models.request.TourCreateRequest;
import com.tourya.api.models.request.TourFullDataRequest;
import com.tourya.api.models.request.TourRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourMapper {
    private final ProviderMapper providerMapper;
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
    public Tour toTour(TourCreateRequest tourCreateRequest){
        Tour tour = new Tour();
        tour.setName(tourCreateRequest.getName());
        tour.setDescription(tourCreateRequest.getDescription());
        tour.setDuration(tourCreateRequest.getDuration());
        tour.setMaxPeople(tourCreateRequest.getMaxPeople());
        tour.setHighlight(tourCreateRequest.getHighlight());
        tour.setMinAge(tourCreateRequest.getMinAge());
        tour.setRating(tourCreateRequest.getRating());
        return tour;
    }
    public Tour toTour(TourFullDataRequest tourFullDataRequest){
        Tour tour = new Tour();
        tour.setName(tourFullDataRequest.getName());
        tour.setDescription(tourFullDataRequest.getDescription());
        tour.setDuration(tourFullDataRequest.getDuration());
        tour.setMaxPeople(tourFullDataRequest.getMaxPeople());
        tour.setHighlight(tourFullDataRequest.getHighlight());
        tour.setMinAge(tourFullDataRequest.getMinAge());
        tour.setRating(tourFullDataRequest.getRating());
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
        tourResponse.setMinAge(tour.getMinAge());
        tourResponse.setRating(tour.getRating());
        tourResponse.setStatus(tour.getStatus());
        tourResponse.setTourCategory(tourCategoryMapper.toTourCategoryResponse(tour.getTourCategory()));
        tourResponse.setProvider(providerMapper.toProviderResponse(tour.getProvider()));
        return tourResponse;
    }
    public TourFullDataResponse toTourFullDataResponse(Tour tour, List<TourAddressResponse> tourAddressResponseList,
                                                       List<TourMainAttractionResponse>  tourMainAttractionResponseList,
                                                       List<TourIncludesExcludesResponse>  tourIncludesResponseList,
                                                       List<TourIncludesExcludesResponse>  tourExcludesResponseList,
                                                       List<TourFaqResponse> tourFaqResponseList,
                                                       List<TourItineraryResponse> tourItineraryResponseList,
                                                       List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList){
        TourFullDataResponse tourFullDataResponse = new TourFullDataResponse();
        tourFullDataResponse.setId(tour.getId());
        tourFullDataResponse.setName(tour.getName());
        tourFullDataResponse.setDescription(tour.getDescription());
        tourFullDataResponse.setDescription(tour.getDescription());
        tourFullDataResponse.setTourCategoryId(tour.getTourCategory().getId());
        tourFullDataResponse.setDuration(tour.getDuration());
        tourFullDataResponse.setMaxPeople(tour.getMaxPeople());
        tourFullDataResponse.setHighlight(tour.getHighlight());
        tourFullDataResponse.setMinAge(tour.getMinAge());
        tourFullDataResponse.setRating(tour.getRating());
        tourFullDataResponse.setStatus(tour.getStatus());
        tourFullDataResponse.setLocations(tourAddressResponseList);
        tourFullDataResponse.setMainAttractions(tourMainAttractionResponseList);
        tourFullDataResponse.setIncludes(tourIncludesResponseList);
        tourFullDataResponse.setExcludes(tourExcludesResponseList);
        tourFullDataResponse.setFaq(tourFaqResponseList);
        tourFullDataResponse.setItineraries(tourItineraryResponseList);
        tourFullDataResponse.setCancellationPolicies(tourCancellationPolicyResponseList);
        return tourFullDataResponse;
    }
    public TourCompleteDataResponse toTourCompleteDataResponse(Tour tour, List<TourAddressResponse> tourAddressResponseList,
                                                       List<TourMainAttractionResponse>  tourMainAttractionResponseList,
                                                       List<TourIncludesExcludesResponse>  tourIncludesResponseList,
                                                       List<TourIncludesExcludesResponse>  tourExcludesResponseList,
                                                       List<TourFaqResponse> tourFaqResponseList,
                                                       List<TourItineraryResponse> tourItineraryResponseList,
                                                       List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList,
                                                       List<TourGalleryResponse>  tourGalleryResponseList){
        TourCompleteDataResponse tourFullDataResponse = new TourCompleteDataResponse();
        tourFullDataResponse.setId(tour.getId());
        tourFullDataResponse.setName(tour.getName());
        tourFullDataResponse.setDescription(tour.getDescription());
        tourFullDataResponse.setDescription(tour.getDescription());
        tourFullDataResponse.setTourCategoryId(tour.getTourCategory().getId());
        tourFullDataResponse.setDuration(tour.getDuration());
        tourFullDataResponse.setMaxPeople(tour.getMaxPeople());
        tourFullDataResponse.setHighlight(tour.getHighlight());
        tourFullDataResponse.setMinAge(tour.getMinAge());
        tourFullDataResponse.setRating(tour.getRating());
        tourFullDataResponse.setStatus(tour.getStatus());
        tourFullDataResponse.setLocations(tourAddressResponseList);
        tourFullDataResponse.setMainAttractions(tourMainAttractionResponseList);
        tourFullDataResponse.setIncludes(tourIncludesResponseList);
        tourFullDataResponse.setExcludes(tourExcludesResponseList);
        tourFullDataResponse.setFaq(tourFaqResponseList);
        tourFullDataResponse.setItineraries(tourItineraryResponseList);
        tourFullDataResponse.setCancellationPolicies(tourCancellationPolicyResponseList);
        tourFullDataResponse.setGalleries(tourGalleryResponseList);
        return tourFullDataResponse;
    }
}

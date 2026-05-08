package com.tourya.api.models.mapper;

import com.tourya.api.models.Tour;
import com.tourya.api.models.responses.*;
import com.tourya.api.models.request.TourCreateRequest;
import com.tourya.api.models.request.TourFullDataRequest;
import com.tourya.api.models.request.TourRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
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
        tour.setPriceType(tourRequest.getPriceType());
        tour.setIsUnlimitedCapacity(tourRequest.getIsUnlimitedCapacity());
        tour.setSubCategory(tourRequest.getSubCategory());
        tour.setDurationEnum(tourRequest.getDurationEnum());
        tour.setTimeOfDay(toTimeOfDayDb(tourRequest.getTimeOfDay()));
        tour.setHighlight(tourRequest.getHighlight());
        return tour;
    }
    public Tour toTour(TourCreateRequest tourCreateRequest){
        Tour tour = new Tour();
        tour.setName(tourCreateRequest.getName());
        tour.setDescription(tourCreateRequest.getDescription());
        tour.setDuration(tourCreateRequest.getDuration());
        tour.setMaxPeople(tourCreateRequest.getMaxPeople());
        tour.setPriceType(tourCreateRequest.getPriceType());
        tour.setIsUnlimitedCapacity(tourCreateRequest.getIsUnlimitedCapacity());
        tour.setSubCategory(tourCreateRequest.getSubCategory());
        tour.setDurationEnum(tourCreateRequest.getDurationEnum());
        tour.setTimeOfDay(toTimeOfDayDb(tourCreateRequest.getTimeOfDay()));
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
        tour.setPriceType(tourFullDataRequest.getPriceType());
        tour.setIsUnlimitedCapacity(tourFullDataRequest.getIsUnlimitedCapacity());
        tour.setSubCategory(tourFullDataRequest.getSubCategory());
        tour.setDurationEnum(tourFullDataRequest.getDurationEnum());
        tour.setTimeOfDay(toTimeOfDayDb(tourFullDataRequest.getTimeOfDay()));
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
        tourResponse.setPriceType(tour.getPriceType());
        tourResponse.setIsUnlimitedCapacity(tour.getIsUnlimitedCapacity());
        tourResponse.setSubCategory(tour.getSubCategory());
        tourResponse.setDurationEnum(tour.getDurationEnum());
        tourResponse.setTimeOfDay(toTimeOfDayApi(tour.getTimeOfDay()));
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
                                                       List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList) {
        return this.toTourFullDataResponse(tour, tourAddressResponseList, tourMainAttractionResponseList,
                tourIncludesResponseList, tourExcludesResponseList, tourFaqResponseList, tourItineraryResponseList,
                tourCancellationPolicyResponseList, null, null);
    }

    public TourFullDataResponse toTourFullDataResponse(Tour tour, List<TourAddressResponse> tourAddressResponseList,
                                                       List<TourMainAttractionResponse>  tourMainAttractionResponseList,
                                                       List<TourIncludesExcludesResponse>  tourIncludesResponseList,
                                                       List<TourIncludesExcludesResponse>  tourExcludesResponseList,
                                                       List<TourFaqResponse> tourFaqResponseList,
                                                       List<TourItineraryResponse> tourItineraryResponseList,
                                                       List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList,
                                                       List<TourGalleryResponse> tourGalleryResponseList) {
        return this.toTourFullDataResponse(
                tour,
                tourAddressResponseList,
                tourMainAttractionResponseList,
                tourIncludesResponseList,
                tourExcludesResponseList,
                tourFaqResponseList,
                tourItineraryResponseList,
                tourCancellationPolicyResponseList,
                tourGalleryResponseList,
                null
        );
    }

    public TourFullDataResponse toTourFullDataResponse(Tour tour, List<TourAddressResponse> tourAddressResponseList,
                                                       List<TourMainAttractionResponse>  tourMainAttractionResponseList,
                                                       List<TourIncludesExcludesResponse>  tourIncludesResponseList,
                                                       List<TourIncludesExcludesResponse>  tourExcludesResponseList,
                                                       List<TourFaqResponse> tourFaqResponseList,
                                                       List<TourItineraryResponse> tourItineraryResponseList,
                                                       List<TourCancellationPolicyResponse>  tourCancellationPolicyResponseList,
                                                       List<TourGalleryResponse> tourGalleryResponseList,
                                                       List<Integer> tagIds) {
        TourFullDataResponse tourFullDataResponse = new TourFullDataResponse();
        tourFullDataResponse.setId(tour.getId());
        tourFullDataResponse.setName(tour.getName());
        tourFullDataResponse.setDescription(tour.getDescription());
        tourFullDataResponse.setTourCategoryId(tour.getTourCategory().getId());
        tourFullDataResponse.setDuration(tour.getDuration());
        tourFullDataResponse.setMaxPeople(tour.getMaxPeople());
        tourFullDataResponse.setPriceType(tour.getPriceType());
        tourFullDataResponse.setIsUnlimitedCapacity(tour.getIsUnlimitedCapacity());
        tourFullDataResponse.setSubCategory(tour.getSubCategory());
        tourFullDataResponse.setDurationEnum(tour.getDurationEnum());
        tourFullDataResponse.setTimeOfDay(toTimeOfDayApi(tour.getTimeOfDay()));
        tourFullDataResponse.setHighlight(tour.getHighlight());
        tourFullDataResponse.setMinAge(tour.getMinAge());
        tourFullDataResponse.setRating(tour.getRating());
        tourFullDataResponse.setStatus(tour.getStatus());
        tourFullDataResponse.setProvider(providerMapper.toProviderResponse(tour.getProvider()));
        tourFullDataResponse.setLocations(tourAddressResponseList);
        tourFullDataResponse.setMainAttractions(tourMainAttractionResponseList);
        tourFullDataResponse.setIncludes(tourIncludesResponseList);
        tourFullDataResponse.setExcludes(tourExcludesResponseList);
        tourFullDataResponse.setFaq(tourFaqResponseList);
        tourFullDataResponse.setItineraries(tourItineraryResponseList);
        tourFullDataResponse.setCancellationPolicies(tourCancellationPolicyResponseList);

        // Conditionally add galleries
        if (tourGalleryResponseList != null) {
            tourFullDataResponse.setGalleries(tourGalleryResponseList);
        }

        if (tagIds != null) {
            tourFullDataResponse.setTagIds(tagIds);
        }

        return tourFullDataResponse;
    }

    private String[] toTimeOfDayDb(List<com.tourya.api.constans.enums.TourTimeOfDayEnum> api) {
        if (api == null) return null;
        return api.stream().map(com.tourya.api.constans.enums.TourTimeOfDayEnum::getValue).toArray(String[]::new);
    }

    private List<com.tourya.api.constans.enums.TourTimeOfDayEnum> toTimeOfDayApi(String[] db) {
        if (db == null) return null;
        return Arrays.stream(db).map(com.tourya.api.constans.enums.TourTimeOfDayEnum::of).toList();
    }
}

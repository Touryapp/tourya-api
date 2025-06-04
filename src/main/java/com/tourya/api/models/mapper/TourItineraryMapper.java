package com.tourya.api.models.mapper;

import com.tourya.api.models.TourItinerary;
import com.tourya.api.models.request.TourItineraryRequest;
import com.tourya.api.models.responses.TourItineraryResponse;
import org.springframework.stereotype.Component;

@Component
public class TourItineraryMapper {

    public TourItinerary toTourItinerary(TourItineraryRequest request) {
        return TourItinerary.builder()
                .title(request.getTitle())
                .day(request.getDay())
                .time(request.getTime())
                .description(request.getDescription())
                .build();
    }

    public TourItineraryResponse toTourItineraryResponse(TourItinerary entity) {
        return new TourItineraryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDay(),
                entity.getTime().toString(),
                entity.getDescription()
        );
    }
}
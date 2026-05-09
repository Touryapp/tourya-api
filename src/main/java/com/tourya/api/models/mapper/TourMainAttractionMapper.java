package com.tourya.api.models.mapper;

import com.tourya.api.models.TourMainAttraction;
import com.tourya.api.models.request.TourMainAttractionRequest;
import com.tourya.api.models.responses.TourMainAttractionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourMainAttractionMapper {

    public TourMainAttraction toTourMainAttraction(TourMainAttractionRequest tourMainAttractionRequest) {
        return TourMainAttraction.builder()
            .description(tourMainAttractionRequest.getDescription())
            .build();
    }
    public TourMainAttractionResponse toTourMainAttractionResponse(TourMainAttraction tourMainAttraction) {
        return new TourMainAttractionResponse(
                tourMainAttraction.getId(),
                tourMainAttraction.getDescription()
        );
    }

    public List<TourMainAttractionResponse> toResponseList(List<TourMainAttraction> list) {
        return list.stream()
                .map(this::toTourMainAttractionResponse)
                .collect(Collectors.toList());
    }

    public void updateTourMainAttractionFromRequest(TourMainAttractionRequest request, TourMainAttraction entity) {
        if (request == null || entity == null) {
            return;
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }
}
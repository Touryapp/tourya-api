package com.tourya.api.models.mapper;

import com.tourya.api.models.TourIncludesExcludes;
import com.tourya.api.models.resquest.TourIncludesExcludesRequest;
import com.tourya.api.models.responses.TourIncludesExcludesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourIncludesExcludesMapper {

    public TourIncludesExcludes toTourIncludesExcludes( TourIncludesExcludesRequest tourIncludesExcludes) {
        return TourIncludesExcludes.builder()
                .description(tourIncludesExcludes.getDescription())
                .type(tourIncludesExcludes.getType())
                .build();
    }

    public  TourIncludesExcludesResponse tourIncludesExcludesResponse(TourIncludesExcludes tourIncludesExcludes) {
        return new TourIncludesExcludesResponse(
                tourIncludesExcludes.getId(),
                tourIncludesExcludes.getDescription(),
                tourIncludesExcludes.getType()
        );
    }
}
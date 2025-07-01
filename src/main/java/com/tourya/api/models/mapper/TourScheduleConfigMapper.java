package com.tourya.api.models.mapper;

import com.tourya.api.models.TourScheduleConfig;
import com.tourya.api.models.request.TourScheduleConfigRequest;
import com.tourya.api.models.responses.TourScheduleConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourScheduleConfigMapper {

    public TourScheduleConfig toTourScheduleConfig(TourScheduleConfigRequest request) {
        TourScheduleConfig tourScheduleConfig = new TourScheduleConfig();
        tourScheduleConfig.setStartDate(request.getStartDate());
        tourScheduleConfig.setEndDate(request.getEndDate());
        tourScheduleConfig.setLabel(request.getLabel());
        tourScheduleConfig.setDaysOfWeek(request.getDaysOfWeek());
        tourScheduleConfig.setIsUnlimitedCapacity(request.getIsUnlimitedCapacity());
        return tourScheduleConfig;
    }

    public TourScheduleConfigResponse toTourScheduleConfigResponse(TourScheduleConfig tourScheduleConfig) {
        TourScheduleConfigResponse response = new TourScheduleConfigResponse();
        response.setId(tourScheduleConfig.getId());
        response.setTourId(tourScheduleConfig.getTour().getId());
        response.setStartDate(tourScheduleConfig.getStartDate());
        response.setEndDate(tourScheduleConfig.getEndDate());
        response.setLabel(tourScheduleConfig.getLabel());
        response.setDaysOfWeek(tourScheduleConfig.getDaysOfWeek());
        response.setIsUnlimitedCapacity(tourScheduleConfig.getIsUnlimitedCapacity());
        return response;
    }
}

package com.tourya.api.models.mapper;

import com.tourya.api.models.TourSchedule;
import com.tourya.api.models.request.TourScheduleRequest;
import com.tourya.api.models.responses.TourScheduleResponse;
import org.springframework.stereotype.Service;

@Service
public class TourScheduleMapper {

    public TourSchedule toTourSchedule(TourScheduleRequest tourScheduleRequest){
        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setScheduleDate(tourScheduleRequest.getScheduleDate());
        tourSchedule.setStartTime(tourScheduleRequest.getStartTime());
        tourSchedule.setEndTime(tourScheduleRequest.getEndTime());
        tourSchedule.setMaxCapacity(tourScheduleRequest.getMaxCapacity());
        tourSchedule.setIsUnlimitedCapacity(tourScheduleRequest.getIsUnlimitedCapacity());
        return tourSchedule;
    }

    public TourScheduleResponse toTourScheduleResponse(TourSchedule tourSchedule){
        TourScheduleResponse tourScheduleResponse = new TourScheduleResponse();
        tourScheduleResponse.setId(tourSchedule.getId());
        tourScheduleResponse.setScheduleDate(tourSchedule.getScheduleDate());
        tourScheduleResponse.setStartTime(tourSchedule.getStartTime());
        tourScheduleResponse.setEndTime(tourSchedule.getEndTime());
        tourScheduleResponse.setMaxCapacity(tourSchedule.getMaxCapacity());
        tourScheduleResponse.setIsUnlimitedCapacity(tourSchedule.getIsUnlimitedCapacity());
        return tourScheduleResponse;
    }
}

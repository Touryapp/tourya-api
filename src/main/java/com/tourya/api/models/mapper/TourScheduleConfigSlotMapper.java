package com.tourya.api.models.mapper;

import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.models.request.TourScheduleConfigSlotRequest;
import com.tourya.api.models.responses.TourScheduleConfigSlotResponse;
import org.springframework.stereotype.Service;

@Service
public class TourScheduleConfigSlotMapper {

    public TourScheduleConfigSlot toTourScheduleConfigSlot(TourScheduleConfigSlotRequest tourScheduleConfigSlotRequest){
        TourScheduleConfigSlot tourScheduleConfigSlot = new TourScheduleConfigSlot();
        tourScheduleConfigSlot.setEndTime(tourScheduleConfigSlotRequest.getEndTime());
        tourScheduleConfigSlot.setStartTime(tourScheduleConfigSlotRequest.getStartTime());
        tourScheduleConfigSlot.setMaxCapacity(tourScheduleConfigSlotRequest.getMaxCapacity());
        tourScheduleConfigSlot.setMinCapacity(tourScheduleConfigSlotRequest.getMinCapacity());
        return tourScheduleConfigSlot;
    }

    public TourScheduleConfigSlotResponse toTourScheduleConfigSlotResponse(TourScheduleConfigSlot tourScheduleConfigSlot){
        TourScheduleConfigSlotResponse tourScheduleConfigSlotResponse = new TourScheduleConfigSlotResponse();
        tourScheduleConfigSlotResponse.setId(tourScheduleConfigSlot.getId());
        tourScheduleConfigSlotResponse.setEndTime(tourScheduleConfigSlot.getEndTime());
        tourScheduleConfigSlotResponse.setStartTime(tourScheduleConfigSlot.getStartTime());
        tourScheduleConfigSlotResponse.setMaxCapacity(tourScheduleConfigSlot.getMaxCapacity());
        tourScheduleConfigSlotResponse.setMinCapacity(tourScheduleConfigSlot.getMinCapacity());
        tourScheduleConfigSlotResponse.setConfigId(tourScheduleConfigSlot.getConfig().getId());
        return tourScheduleConfigSlotResponse;
    }
}

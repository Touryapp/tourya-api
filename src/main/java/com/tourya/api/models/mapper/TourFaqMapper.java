package com.tourya.api.models.mapper;

import com.tourya.api.models.TourFaq;
import com.tourya.api.models.responses.TourFaqResponse;
import com.tourya.api.models.resquest.TourFaqRequest;
import org.springframework.stereotype.Service;

@Service
public class TourFaqMapper {

    public TourFaq toTourFaq(TourFaqRequest tourFaqRequest){
        TourFaq tourFaq = new TourFaq();
        tourFaq.setQuestion(tourFaqRequest.getQuestion());
        tourFaq.setAnswer(tourFaqRequest.getAnswer());
        return tourFaq;
    }

    public TourFaqResponse toTourFaqResponse(TourFaq tourFaq){
        TourFaqResponse tourFaqResponse = new TourFaqResponse();
        tourFaqResponse.setId(tourFaq.getId());
        tourFaqResponse.setQuestion(tourFaq.getQuestion());
        tourFaqResponse.setAnswer(tourFaq.getAnswer());
        return tourFaqResponse;
    }
}

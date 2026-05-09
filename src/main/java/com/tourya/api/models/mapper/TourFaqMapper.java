package com.tourya.api.models.mapper;

import com.tourya.api.models.TourFaq;
import com.tourya.api.models.responses.TourFaqResponse;
import com.tourya.api.models.request.TourFaqRequest;
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

    public void updateTourFaqFromRequest(TourFaqRequest request, TourFaq entity) {
        if (request == null || entity == null) {
            return;
        }
        if (request.getQuestion() != null) {
            entity.setQuestion(request.getQuestion());
        }
        if (request.getAnswer() != null) {
            entity.setAnswer(request.getAnswer());
        }
    }
}

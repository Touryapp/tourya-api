package com.tourya.api.models.mapper;


import com.tourya.api.models.TourCancellationPolicy;
import com.tourya.api.models.request.TourCancellationPolicyRequest;
import com.tourya.api.models.responses.TourCancellationPolicyResponse;
import org.springframework.stereotype.Service;

@Service
public class TourCancellationPolicyMapper {

    public TourCancellationPolicy toTourCancellationPolicy(TourCancellationPolicyRequest tourCancellationPolicyRequest){
        TourCancellationPolicy tourCancellationPolicy = new TourCancellationPolicy();
        tourCancellationPolicy.setObservations(tourCancellationPolicyRequest.getObservations());
        tourCancellationPolicy.setAllowsRainRefund(tourCancellationPolicyRequest.isAllowsRainRefund());
        tourCancellationPolicy.setAllowsRescheduling(tourCancellationPolicyRequest.isAllowsRescheduling());
        tourCancellationPolicy.setCancellationPolicyType(tourCancellationPolicyRequest.getCancellationPolicyType());
        return tourCancellationPolicy;
    }

    public TourCancellationPolicyResponse toTourCancellationPolicyResponse(TourCancellationPolicy tourCancellationPolicy){
        TourCancellationPolicyResponse tourCancellationPolicyResponse = new TourCancellationPolicyResponse();
        tourCancellationPolicyResponse.setId(tourCancellationPolicy.getId());
        tourCancellationPolicyResponse.setObservations(tourCancellationPolicy.getObservations());
        tourCancellationPolicyResponse.setAllowsRainRefund(tourCancellationPolicy.isAllowsRainRefund());
        tourCancellationPolicyResponse.setAllowsRescheduling(tourCancellationPolicy.isAllowsRescheduling());
        tourCancellationPolicyResponse.setCancellationPolicyType(tourCancellationPolicy.getCancellationPolicyType());
        return tourCancellationPolicyResponse;
    }
}

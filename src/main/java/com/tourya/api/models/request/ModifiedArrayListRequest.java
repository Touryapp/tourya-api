package com.tourya.api.models.request;


import lombok.Data;

@Data
public class ModifiedArrayListRequest {
    boolean updatedLocations = Boolean.FALSE;
    boolean updatedMainAttractions = Boolean.FALSE;
    boolean updatedIncludes = Boolean.FALSE;
    boolean updatedExcludes = Boolean.FALSE;
    boolean updatedItineraries = Boolean.FALSE;
    boolean updatedFaq = Boolean.FALSE;
    boolean updatedCancellationPolicies = Boolean.FALSE;
}

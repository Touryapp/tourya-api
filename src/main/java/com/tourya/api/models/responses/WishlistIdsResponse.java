package com.tourya.api.models.responses;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class WishlistIdsResponse {
    @Builder.Default
    private List<Integer> tourIds = new ArrayList<>();
}


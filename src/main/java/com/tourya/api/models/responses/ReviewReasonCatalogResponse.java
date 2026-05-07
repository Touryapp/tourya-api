package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.ReviewReasonTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ReviewReasonCatalogResponse {

    @Builder.Default
    private List<Item> positive = new ArrayList<>();

    @Builder.Default
    private List<Item> negative = new ArrayList<>();

    @Data
    @Builder
    public static class Item {
        private Integer id; // 1..7 dentro de su tipo
        private String label;
        private ReviewReasonTypeEnum type;
    }
}


package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourIncludesExcludesResponse {
    private Integer id;
    private String description;
    private IncludeExcludeTypeEnum type;
}
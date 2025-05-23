package com.tourya.api.models.resquest;

import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourIncludesExcludesRequest {
    private String description;
    private IncludeExcludeTypeEnum type;
}
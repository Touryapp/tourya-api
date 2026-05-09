package com.tourya.api.models.responses;

import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.models.TranslatedField;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourIncludesExcludesResponse {
    private Integer id;
    private TranslatedField description;
    private IncludeExcludeTypeEnum type;
}
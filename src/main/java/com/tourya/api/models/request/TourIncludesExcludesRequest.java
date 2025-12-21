package com.tourya.api.models.request;

import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import com.tourya.api.models.TranslatedField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourIncludesExcludesRequest {
    private Integer id;
    @Valid
    @NotNull(message = "description is mandatory")
    private TranslatedField description;

    @NotNull(message = "type is mandatory")
    private IncludeExcludeTypeEnum type;
}
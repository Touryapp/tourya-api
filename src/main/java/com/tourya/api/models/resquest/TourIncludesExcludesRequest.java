package com.tourya.api.models.resquest;

import com.tourya.api.constans.enums.IncludeExcludeTypeEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourIncludesExcludesRequest {

    @NotEmpty(message = "description is mandatory")
    @NotNull(message = "description is mandatory")
    private String description;

    @NotNull(message = "type is mandatory")
    private IncludeExcludeTypeEnum type;
}
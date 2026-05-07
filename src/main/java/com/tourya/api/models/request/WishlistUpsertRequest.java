package com.tourya.api.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WishlistUpsertRequest {
    @Schema(description = "ID del tour a agregar/quitar de wishlist", example = "43")
    private Integer tourId;
}


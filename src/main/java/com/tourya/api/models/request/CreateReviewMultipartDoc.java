package com.tourya.api.models.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Solo para documentación OpenAPI/Swagger.
 * El endpoint real recibe `reviewData` como string JSON y `files` como multipart.
 */
@Data
@Schema(
        name = "CreateReviewMultipart",
        description = "Cuerpo multipart/form-data para crear una reseña. `reviewData` es un JSON (CreateReviewRequest)."
)
public class CreateReviewMultipartDoc {

    @Schema(
            description = "Objeto JSON con los datos de la reseña (lo que viaja dentro del part `reviewData`).",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private CreateReviewRequest reviewData;

    @ArraySchema(
            schema = @Schema(type = "string", format = "binary", description = "Archivo de imagen"),
            arraySchema = @Schema(description = "Imágenes (máximo 5)")
    )
    private List<String> files;
}


package com.tourya.api.agents.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * IA-07: request de {@code POST /agents/operator-support/validate-gallery}.
 *
 * <p>El frontend envia metadata de las imagenes ANTES de hacer upload real —
 * evita subir bytes que van a fallar la validacion de {@code GalleryValidator}
 * en el sync (RN-013).</p>
 *
 * <p>No hay LLM call en este endpoint — se aprovecha la logica existente de
 * {@link com.tourya.api.services.GalleryValidator} traducida a issues
 * estructurados para el operador.</p>
 */
@Data
public class ValidateGalleryRequest {

    @NotNull(message = "images es obligatorio")
    @NotEmpty(message = "images no puede estar vacio")
    @Valid
    @Schema(description = "Metadata de las imagenes que el operador quiere subir.", required = true)
    private List<ImageMetadata> images;

    /**
     * @param filename  Nombre original del archivo (opcional pero recomendado para trazabilidad).
     * @param sizeBytes Tamano en bytes del archivo.
     * @param widthPx   Ancho en pixeles.
     * @param heightPx  Alto en pixeles.
     * @param format    Extension o mime-type (jpeg / jpg / png / webp).
     */
    @Data
    public static class ImageMetadata {
        private String filename;
        @NotNull
        private Long sizeBytes;
        @NotNull
        private Integer widthPx;
        @NotNull
        private Integer heightPx;
        @NotNull
        private String format;
    }
}

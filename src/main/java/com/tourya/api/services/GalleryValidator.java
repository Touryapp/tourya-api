package com.tourya.api.services;

import com.tourya.api.constans.enums.ConfigKeyEnum;
import com.tourya.api.exceptions.GalleryValidationException;
import com.tourya.api.exceptions.GalleryValidationException.GalleryValidationIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Valida las imagenes que llegan a los endpoints de sync de galeria de tour (RN-013):
 * <ul>
 *   <li>Formato: JPEG, PNG o WebP.</li>
 *   <li>Tamano: hasta GALLERY_MAX_SIZE_MB (default 5).</li>
 *   <li>Orientacion: landscape (ancho >= alto).</li>
 *   <li>Calidad minima: ancho >= GALLERY_MIN_WIDTH_PX (default 800).</li>
 *   <li>Cuenta: total final de imagenes por tour <= GALLERY_MAX_IMAGES_PER_TOUR (default 7).</li>
 * </ul>
 * Umbrales configurables via {@code app_config} — ADMIN los ajusta con PUT /config/{key}
 * sin necesidad de re-deploy.
 * <p>Al detectar issues acumula toda la lista antes de lanzar (all-or-nothing): el frontend
 * recibe el detalle completo en una sola respuesta para mostrar todos los errores al usuario.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GalleryValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final AppConfigService appConfigService;

    /**
     * Valida cuenta total y cada archivo nuevo. Lanza GalleryValidationException con la
     * lista completa si hay uno o mas issues.
     *
     * @param newFiles         archivos nuevos que llegan en el sync (puede estar vacio)
     * @param resultingTotal   cuenta total resultante DESPUES del sync (existentes que se
     *                         mantienen + nuevos). Se calcula fuera del validador y se pasa
     *                         para no acoplar el validador con la logica de merge de sync.
     */
    public void validateSync(List<MultipartFile> newFiles, int resultingTotal) {
        List<GalleryValidationIssue> issues = new ArrayList<>();

        int maxImagesPerTour = appConfigService.getInt(ConfigKeyEnum.GALLERY_MAX_IMAGES_PER_TOUR, 7);
        if (resultingTotal > maxImagesPerTour) {
            issues.add(new GalleryValidationIssue(
                    null,
                    "TOO_MANY_IMAGES",
                    "El tour tendria " + resultingTotal + " imagenes, excede el maximo de " + maxImagesPerTour
            ));
        }

        if (newFiles != null) {
            long maxSizeMb = appConfigService.getInt(ConfigKeyEnum.GALLERY_MAX_SIZE_MB, 5);
            int minWidthPx = appConfigService.getInt(ConfigKeyEnum.GALLERY_MIN_WIDTH_PX, 800);
            long maxSizeBytes = maxSizeMb * BYTES_PER_MB;

            for (MultipartFile file : newFiles) {
                validateFile(file, maxSizeBytes, maxSizeMb, minWidthPx, issues);
            }
        }

        if (!issues.isEmpty()) {
            throw new GalleryValidationException(issues);
        }
    }

    private void validateFile(MultipartFile file, long maxSizeBytes, long maxSizeMb, int minWidthPx, List<GalleryValidationIssue> issues) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "<sin nombre>";

        if (file.isEmpty()) {
            issues.add(new GalleryValidationIssue(name, "EMPTY_FILE", "El archivo esta vacio"));
            return;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            issues.add(new GalleryValidationIssue(name, "INVALID_FORMAT",
                    "Formato no soportado (recibido: " + contentType + "). Aceptados: JPEG, PNG, WebP"));
            return;
        }

        if (file.getSize() > maxSizeBytes) {
            long actualMb = Math.round(file.getSize() * 10.0 / BYTES_PER_MB) / 10;
            issues.add(new GalleryValidationIssue(name, "TOO_LARGE",
                    "Tamano " + actualMb + " MB excede el maximo de " + maxSizeMb + " MB"));
            return;
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                issues.add(new GalleryValidationIssue(name, "UNREADABLE_IMAGE",
                        "No se pudo leer el archivo como imagen"));
                return;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (width < height) {
                issues.add(new GalleryValidationIssue(name, "NOT_LANDSCAPE",
                        "Orientacion vertical (" + width + "x" + height + "). Requerido: horizontal (ancho >= alto)"));
                return;
            }
            if (width < minWidthPx) {
                issues.add(new GalleryValidationIssue(name, "WIDTH_TOO_SMALL",
                        "Ancho " + width + "px es menor al minimo de " + minWidthPx + "px"));
            }
        } catch (IOException e) {
            log.warn("Error leyendo imagen '{}': {}", name, e.getMessage());
            issues.add(new GalleryValidationIssue(name, "READ_ERROR",
                    "Error al leer el archivo: " + e.getMessage()));
        }
    }
}

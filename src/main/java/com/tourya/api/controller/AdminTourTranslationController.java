package com.tourya.api.controller;

import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.repository.TourRepository;
import com.tourya.api.services.translation.ITranslationService;
import com.tourya.api.services.translation.TourTranslationApplier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * IA-09: endpoint admin para re-disparar la traduccion es -> en / pt-BR de un
 * tour existente. Uso previsto:
 *
 * <ul>
 *   <li><b>Backfill</b>: tours legacy creados antes del deploy que no tienen
 *       en/pt (rellena todos los campos vacios).</li>
 *   <li><b>Testing</b>: verificar el pipeline end-to-end desde consola /
 *       Postman sin tener que crear un tour desde cero.</li>
 *   <li><b>Recovery</b>: re-generar traducciones si el listener original fallo
 *       (Google Translate 500, ADC roto en dev, budget exhausto, etc.).</li>
 * </ul>
 *
 * <p>Solo ADMIN — no hay coste por operar sobre tour ajeno, pero mantener
 * consistente con {@link AdminJobsController}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/admin/tours")
@RequiredArgsConstructor
@Tag(name = "Admin Tour Translation (IA-09)",
        description = "Re-disparar traduccion es -> en / pt-BR de un tour (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class AdminTourTranslationController {

    private final TourRepository tourRepository;
    private final TourTranslationApplier translationApplier;
    private final ITranslationService translationService;

    @PostMapping("/{tourId}/retranslate")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Re-traducir un tour (es -> en / pt-BR) con Google Cloud Translation",
            description = "Delega en TourTranslationApplier con el mismo guardrail que el flujo automatico: "
                    + "solo rellena los campos en/pt vacios, respeta lo que el provider haya escrito. "
                    + "El call se lanza async — la respuesta HTTP confirma el dispatch, no el resultado. "
                    + "Solo ADMIN.")
    public ResponseEntity<Map<String, Object>> retranslate(@PathVariable("tourId") Integer tourId) {
        tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: id=" + tourId));

        boolean serviceEnabled = translationService.isEnabled();
        log.info("Admin trigger IA-09 retranslate tourId={} translationEnabled={}", tourId, serviceEnabled);
        translationApplier.translateTourAsync(tourId);

        return ResponseEntity.ok(Map.of(
                "tourId", tourId,
                "translationEnabled", serviceEnabled,
                "dispatched", true,
                "message", serviceEnabled
                        ? "Traduccion asincrona lanzada — revisar logs Cloud Run para el resultado."
                        : "Servicio de traduccion deshabilitado (agents.translation.enabled=false). "
                                + "El call no persistio cambios."
        ));
    }
}

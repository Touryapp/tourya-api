package com.tourya.api.controller;

import com.tourya.api.jobs.PendingReservationNoShowJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TC-011 (#206 reabierto Luis 2026-08-04): endpoint temporal para disparar jobs
 * scheduled manualmente. Permite validar el fix sin esperar la corrida diaria.
 *
 * <p>Restringido a ADMIN via {@link PreAuthorize}. Considerar removerlo o
 * mantenerlo como herramienta interna de operaciones — es utilidad, no
 * feature funcional del negocio.</p>
 */
@Slf4j
@RestController
@RequestMapping("/admin/jobs")
@RequiredArgsConstructor
@Tag(name = "Admin Jobs", description = "Trigger manual de jobs scheduled (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class AdminJobsController {

    private final PendingReservationNoShowJob pendingReservationNoShowJob;

    @PostMapping("/no-show/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Disparar PendingReservationNoShowJob manualmente",
            description = "Ejecuta ahora el job que marca NO_SHOW las reservas PENDING/RESCHEDULED con scheduleDate < hoy. Solo ADMIN.")
    public ResponseEntity<String> triggerNoShowJob() {
        log.info("Admin trigger manual: PendingReservationNoShowJob.markNoShows()");
        pendingReservationNoShowJob.markNoShows();
        return ResponseEntity.ok("PendingReservationNoShowJob ejecutado. Revisar logs Cloud Run para el resultado.");
    }
}

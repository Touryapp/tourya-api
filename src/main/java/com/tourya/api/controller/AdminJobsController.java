package com.tourya.api.controller;

import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.jobs.PendingReservationNoShowJob;
import com.tourya.api.models.MaritimActivityReport;
import com.tourya.api.repository.MaritimActivityReportRepository;
import com.tourya.api.services.ReservationService;
import com.tourya.api.services.maritime.events.MaritimeAlertCreatedEvent;
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
    private final ReservationService reservationService;
    private final MaritimActivityReportRepository maritimActivityReportRepository;

    @PostMapping("/no-show/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Disparar PendingReservationNoShowJob manualmente",
            description = "Ejecuta ahora el job que marca NO_SHOW las reservas PENDING/RESCHEDULED con scheduleDate < hoy. Solo ADMIN.")
    public ResponseEntity<String> triggerNoShowJob() {
        log.info("Admin trigger manual: PendingReservationNoShowJob.markNoShows()");
        pendingReservationNoShowJob.markNoShows();
        return ResponseEntity.ok("PendingReservationNoShowJob ejecutado. Revisar logs Cloud Run para el resultado.");
    }

    /**
     * TC-018 (#227 reabierto): trigger sincrono del hook DIMAR para un reporte especifico.
     * Ejecuta {@code cancelAffectedByRedAlert} en el mismo hilo (no async) para diagnostico
     * y para re-procesar reportes cuyo listener original haya fallado o no se disparo
     * (p.ej. reporte creado antes del deploy del fix).
     */
    @PostMapping("/dimar-alert/{reportId}/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Re-procesar hook DIMAR de un reporte especifico",
            description = "Ejecuta sincronamente cancelAffectedByRedAlert para el reporte dado. Solo ADMIN.")
    public ResponseEntity<String> triggerDimarAlert(@PathVariable("reportId") Long reportId) {
        MaritimActivityReport report = maritimActivityReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Maritime report not found: " + reportId));

        MaritimeAlertCreatedEvent event = new MaritimeAlertCreatedEvent(
                report.getId(),
                report.getSubcategoryCode(),
                report.getCountry() != null ? report.getCountry().getId() : null,
                report.getState() != null ? report.getState().getId() : null,
                report.getCity() != null ? report.getCity().getId() : null,
                report.getReportStartDate(),
                report.getReportEndDate(),
                report.getFlag());

        log.info("Admin trigger manual DIMAR alert: reportId={}, subcat={}, country={}, state={}, city={}, dates={}..{}, flag={}",
                event.reportId(), event.subcategoryCode(), event.countryId(),
                event.stateId(), event.cityId(), event.startDate(), event.endDate(), event.flag());

        reservationService.cancelAffectedByRedAlert(event);
        return ResponseEntity.ok("Hook DIMAR ejecutado para reporte " + reportId + ". Revisar logs Cloud Run.");
    }
}

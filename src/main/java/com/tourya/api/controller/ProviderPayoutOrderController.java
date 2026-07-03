package com.tourya.api.controller;

import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import com.tourya.api.models.responses.ProviderPayoutOrderDetailsResponse;
import com.tourya.api.models.responses.ProviderPayoutOrderListPageResponse;
import com.tourya.api.services.ProviderPayoutOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/provider/payout-orders")
@RequiredArgsConstructor
@Tag(
        name = "Provider Payout Orders",
        description = """
                Pagos al proveedor por reservas entregadas (DELIVERED).

                **Capas de estado (no son lo mismo):**
                - `provider_payout_order.status` (PAID | PENDING | CANCELED): estado del **lote/transferencia** \
                agrupada por el job (lun/jue → pago mar/vie). Usar en listados y operación backoffice.
                - `reservation.payout_status` (PENDING | PAID): estado de **pago al proveedor por reserva**. \
                Se copia a PAID cuando backoffice marca la orden pagada (POST .../proof).
                - `account_payable.delivery_status`: deuda contable por reserva; el job de órdenes lee esta tabla.

                **Flujo:** tour consumido → account_payable PENDING → job crea orden PENDING → \
                admin sube comprobante → orden PAID + account_payable PAID + reservation.payout_status PAID.

                Mientras la orden está PENDING, las reservas incluidas siguen con payout_status PENDING \
                (no hay estado intermedio "en cola").
                """
)
public class ProviderPayoutOrderController {

    private final ProviderPayoutOrderService providerPayoutOrderService;

    // Provider: list + details (solo consulta)
    @GetMapping
    @Operation(
            operationId = "providerListPayoutOrders",
            summary = "Listar órdenes de pago (proveedor)",
            description = "Respuesta con orders, paidTotal, pendingTotal, canceledTotal y totalIncome (PAID+PENDING). "
                    + "Filtros opcionales: status, fromDate, toDate.")
    public ResponseEntity<ProviderPayoutOrderListPageResponse> listForProvider(
            Authentication connectedUser,
            @Parameter(description = "PAID | PENDING | CANCELED")
            @RequestParam(value = "status", required = false) ProviderPayoutOrderStatusEnum status,
            @Parameter(description = "Fecha inicio (inclusive)")
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Fecha fin (inclusive)")
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(providerPayoutOrderService.listForProvider(connectedUser, status, fromDate, toDate));
    }

    @GetMapping("/{orderId}")
    @Operation(
            operationId = "providerGetPayoutOrderDetails",
            summary = "Detalle de orden de pago (proveedor)",
            description = "Incluye `status` de la **orden (lote)** y, por cada reserva, `payoutStatus` \
                    (estado de pago de esa reserva). Ver tag del controlador para la diferencia entre ambos."
    )
    public ResponseEntity<ProviderPayoutOrderDetailsResponse> detailsForProvider(
            @PathVariable("orderId") Long orderId,
            Authentication connectedUser) {
        return ResponseEntity.ok(providerPayoutOrderService.getDetailsForProvider(orderId, connectedUser));
    }

    // Backoffice/Admin: list + details + upload+mark paid (editable: archivo adjunto)
    @GetMapping("/admin")
    @Operation(
            operationId = "adminListPayoutOrders",
            summary = "Listar órdenes de pago (backoffice)",
            description = "Igual que proveedor, con filtro opcional providerId y totales por estado.")
    public ResponseEntity<ProviderPayoutOrderListPageResponse> listForAdmin(
            Authentication connectedUser,
            @RequestParam(value = "providerId", required = false) Integer providerId,
            @Parameter(description = "PAID | PENDING | CANCELED")
            @RequestParam(value = "status", required = false) ProviderPayoutOrderStatusEnum status,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(
                providerPayoutOrderService.listForAdmin(connectedUser, providerId, status, fromDate, toDate));
    }

    @GetMapping("/admin/{orderId}")
    @Operation(
            operationId = "adminGetPayoutOrderDetails",
            summary = "Detalle de orden de pago (backoffice)",
            description = "Igual que proveedor. `status` = lote; `reservations[].payoutStatus` = cada reserva."
    )
    public ResponseEntity<ProviderPayoutOrderDetailsResponse> detailsForAdmin(
            @PathVariable("orderId") Long orderId,
            Authentication connectedUser) {
        return ResponseEntity.ok(providerPayoutOrderService.getDetailsForAdmin(orderId, connectedUser));
    }

    @PostMapping(value = "/admin/{orderId}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "adminUploadPayoutProofAndMarkPaid",
            summary = "Subir comprobante y marcar orden como pagada (backoffice)",
            description = "Marca la orden PAID y propaga a todas sus reservas: account_payable PAID, \
                    reservation.payout_status PAID y payout_paid_at. Solo órdenes en PENDING."
    )
    public ResponseEntity<ProviderPayoutOrderDetailsResponse> uploadProofAndMarkPaid(
            @PathVariable("orderId") Long orderId,
            @RequestPart("file") MultipartFile file,
            Authentication connectedUser) throws IOException {
        return ResponseEntity.ok(providerPayoutOrderService.uploadAttachmentAndMarkPaid(orderId, file, connectedUser));
    }
}


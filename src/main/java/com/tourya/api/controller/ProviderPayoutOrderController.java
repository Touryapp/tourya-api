package com.tourya.api.controller;

import com.tourya.api.models.responses.ProviderPayoutOrderDetailsResponse;
import com.tourya.api.models.responses.ProviderPayoutOrderListItemResponse;
import com.tourya.api.services.ProviderPayoutOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/provider/payout-orders")
@RequiredArgsConstructor
@Tag(name = "Provider Payout Orders")
public class ProviderPayoutOrderController {

    private final ProviderPayoutOrderService providerPayoutOrderService;

    // Provider: list + details (solo consulta)
    @GetMapping
    @Operation(operationId = "providerListPayoutOrders", summary = "Listar órdenes de pago (proveedor)")
    public ResponseEntity<List<ProviderPayoutOrderListItemResponse>> listForProvider(Authentication connectedUser) {
        return ResponseEntity.ok(providerPayoutOrderService.listForProvider(connectedUser));
    }

    @GetMapping("/{orderId}")
    @Operation(operationId = "providerGetPayoutOrderDetails", summary = "Detalle de orden de pago (proveedor)")
    public ResponseEntity<ProviderPayoutOrderDetailsResponse> detailsForProvider(
            @PathVariable Long orderId,
            Authentication connectedUser) {
        return ResponseEntity.ok(providerPayoutOrderService.getDetailsForProvider(orderId, connectedUser));
    }

    // Backoffice/Admin: list + details + upload+mark paid (editable: archivo adjunto)
    @GetMapping("/admin")
    @Operation(operationId = "adminListPayoutOrders", summary = "Listar órdenes de pago (backoffice)")
    public ResponseEntity<List<ProviderPayoutOrderListItemResponse>> listForAdmin(Authentication connectedUser) {
        return ResponseEntity.ok(providerPayoutOrderService.listForAdmin(connectedUser));
    }

    @GetMapping("/admin/{orderId}")
    @Operation(operationId = "adminGetPayoutOrderDetails", summary = "Detalle de orden de pago (backoffice)")
    public ResponseEntity<ProviderPayoutOrderDetailsResponse> detailsForAdmin(
            @PathVariable Long orderId,
            Authentication connectedUser) {
        return ResponseEntity.ok(providerPayoutOrderService.getDetailsForAdmin(orderId, connectedUser));
    }

    @PostMapping(value = "/admin/{orderId}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "adminUploadPayoutProofAndMarkPaid", summary = "Subir comprobante y marcar orden como pagada (backoffice)")
    public ResponseEntity<ProviderPayoutOrderDetailsResponse> uploadProofAndMarkPaid(
            @PathVariable Long orderId,
            @RequestPart("file") MultipartFile file,
            Authentication connectedUser) throws IOException {
        return ResponseEntity.ok(providerPayoutOrderService.uploadAttachmentAndMarkPaid(orderId, file, connectedUser));
    }
}


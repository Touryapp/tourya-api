package com.tourya.api.controller;

import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.responses.CreditResponse;
import com.tourya.api.services.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * TC-022 (#253): endpoints backoffice/admin del flujo de devolucion de creditos.
 * Guard de rol (ADMIN + BACKOFFICE_OPERATION) en el service via Utils.isTouryaBackoffice.
 */
@Slf4j
@RestController
@RequestMapping("/admin/credits")
@RequiredArgsConstructor
@Tag(
        name = "Admin Credits",
        description = "Gestion global de creditos por backoffice: listado y devolucion en efectivo (TC-022 #253)."
)
public class AdminCreditController {

    private final CreditService creditService;

    @GetMapping
    @Operation(
            operationId = "adminListCredits",
            summary = "Listar creditos (backoffice global, TC-022 #253)",
            description = "Retorna un Page<CreditResponse> con datos del turista (touristName, touristEmail). "
                    + "Filtro opcional por status. Solo ADMIN o BACKOFFICE_OPERATION."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado retornado"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    })
    public ResponseEntity<Page<CreditResponse>> listAll(
            Authentication authentication,
            @Parameter(description = "Filtrar por estado (opcional)")
            @RequestParam(value = "status", required = false) CreditStatusEnum status,
            @ParameterObject
            @Parameter(description = "Paginacion via page, size, sort")
            Pageable pageable) {
        return ResponseEntity.ok(creditService.findAllForAdmin(authentication, status, pageable));
    }

    @PostMapping(value = "/{creditId}/upload-refund-proof",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "adminUploadCreditRefundProof",
            summary = "Subir comprobante y marcar devolucion como REFUNDED (TC-022 #253)",
            description = "Transicion REFUND_REQUESTED -> REFUNDED. Multipart form-data con campo 'proof' "
                    + "(JPG/PNG o PDF, max 5MB). Solo ADMIN o BACKOFFICE_OPERATION."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devolucion completada"),
            @ApiResponse(responseCode = "400", description = "Transicion o archivo invalido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Credito no existe")
    })
    public ResponseEntity<CreditResponse> uploadRefundProof(
            @PathVariable("creditId") Long creditId,
            @RequestPart("proof") MultipartFile proof,
            Authentication authentication) throws IOException {
        return ResponseEntity.ok(creditService.uploadRefundProof(creditId, proof, authentication));
    }
}

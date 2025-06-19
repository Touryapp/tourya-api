package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.models.request.RequestProviderDocumentTypeRequest;
import com.tourya.api.models.responses.RequestProviderDocumentTypeResponse;
import com.tourya.api.models.responses.TourCancelCategoryResponse;
import com.tourya.api.services.RequestProviderDocumentTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("requestProviderDocumentType")
@RequiredArgsConstructor
@Tag(name = "RequestProviderDocumentType")
public class RequestProviderDocumentTypeController {

    private final RequestProviderDocumentTypeService requestProviderDocumentTypeService;

    @PostMapping("/admin/save")
    public ResponseEntity<RequestProviderDocumentTypeResponse> save(
            @Valid @RequestBody RequestProviderDocumentTypeRequest requestProviderDocumentTypeRequest,
            Authentication connectedUser){
        return ResponseEntity.ok(requestProviderDocumentTypeService.save(requestProviderDocumentTypeRequest, connectedUser));
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<RequestProviderDocumentTypeResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return  ResponseEntity.ok(requestProviderDocumentTypeService.findAll(page, size, connectedUser));
    }

    @GetMapping("/user/getAllRequestProviderDocumentTypeList")
    public ResponseEntity<List<RequestProviderDocumentTypeResponse>> getAllRequestProviderDocumentTypeList() {
        return ResponseEntity.ok(requestProviderDocumentTypeService.getAllRequestProviderDocumentTypeList());
    }
}

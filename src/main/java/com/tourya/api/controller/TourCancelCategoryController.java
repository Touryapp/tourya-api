package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.models.TourCancelCategory;
import com.tourya.api.models.request.TourCancelCategoryRequest;
import com.tourya.api.models.responses.TourCancelCategoryResponse;
import com.tourya.api.services.TourCancelCategoryService;
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
@RequestMapping("tourCancelCategory")
@RequiredArgsConstructor
@Tag(name = "TourCancelCategory")
public class TourCancelCategoryController {
    private final TourCancelCategoryService tourCancelCategoryService;

    @PostMapping("/admin/save")
    public ResponseEntity<TourCancelCategoryResponse> save(
            @Valid @RequestBody TourCancelCategoryRequest tourCancelCategoryRequest,
            Authentication connectedUser){
        return ResponseEntity.ok(tourCancelCategoryService.save(tourCancelCategoryRequest, connectedUser));
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<TourCancelCategoryResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return  ResponseEntity.ok(tourCancelCategoryService.findAll(page, size, connectedUser));
    }

    @GetMapping("/user/getAllTourCancelCategoryList")
    public ResponseEntity<List<TourCancelCategoryResponse>> getAllTourCategoryList() {
        return ResponseEntity.ok(tourCancelCategoryService.getAllTourCancelCategoryList());
    }
}

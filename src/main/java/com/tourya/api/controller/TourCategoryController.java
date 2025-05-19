package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.models.responses.TourCategoryResponse;
import com.tourya.api.models.resquest.TourCategoryRequest;
import com.tourya.api.services.TourCategoryService;
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
@RequestMapping("tourCategory")
@RequiredArgsConstructor
@Tag(name = "TourCategory")
public class TourCategoryController {

    private final TourCategoryService tourCategoryService;

    @PostMapping("/admin/save")
    public ResponseEntity<TourCategoryResponse> save(
            @Valid @RequestBody TourCategoryRequest tourCategoryRequest,
            Authentication connectedUser){
        return ResponseEntity.ok(tourCategoryService.save(tourCategoryRequest, connectedUser));
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<TourCategoryResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){

        return ResponseEntity.ok(tourCategoryService.findAll(page, size, connectedUser));
    }

    @GetMapping("/user/getAllTourCategoryList")
    public ResponseEntity<List<TourCategoryResponse>> getAllTourCategoryList(){
        return ResponseEntity.ok(tourCategoryService.getTourCategoryList());
    }
    //List<TourCategoryResponse>
}

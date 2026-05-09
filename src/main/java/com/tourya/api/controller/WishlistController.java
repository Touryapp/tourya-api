package com.tourya.api.controller;

import com.tourya.api.models.request.PublicTourScheduleSearchRequest;
import com.tourya.api.models.request.WishlistUpsertRequest;
import com.tourya.api.models.responses.SearchTourScheduleFullResponse;
import com.tourya.api.models.responses.WishlistIdsResponse;
import com.tourya.api.services.SearchTourScheduleFullService;
import com.tourya.api.services.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final SearchTourScheduleFullService searchTourScheduleFullService;

    @GetMapping
    @Operation(operationId = "wishlistGetIds", summary = "Obtener wishlist (IDs de tours)")
    public ResponseEntity<WishlistIdsResponse> getWishlist(Authentication connectedUser) {
        List<Integer> ids = wishlistService.getMyWishlistTourIds(connectedUser);
        return ResponseEntity.ok(WishlistIdsResponse.builder().tourIds(ids).build());
    }

    @PostMapping
    @Operation(operationId = "wishlistAdd", summary = "Agregar tour a wishlist")
    public ResponseEntity<Void> add(@RequestBody WishlistUpsertRequest request, Authentication connectedUser) {
        wishlistService.addToWishlist(request.getTourId(), connectedUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(operationId = "wishlistRemove", summary = "Quitar tour de wishlist")
    public ResponseEntity<Void> remove(@RequestBody WishlistUpsertRequest request, Authentication connectedUser) {
        wishlistService.removeFromWishlist(request.getTourId(), connectedUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    @Operation(operationId = "wishlistSearch", summary = "Buscar tours de mi wishlist (reutiliza el search)")
    public ResponseEntity<Page<SearchTourScheduleFullResponse>> searchWishlist(
            @RequestBody(required = false) PublicTourScheduleSearchRequest filters,
            @ParameterObject Pageable pageable,
            Authentication connectedUser
    ) {
        if (filters == null) filters = new PublicTourScheduleSearchRequest();
        filters.setType("wishlist");
        filters.setTourIds(wishlistService.getMyWishlistTourIds(connectedUser));
        return ResponseEntity.ok(searchTourScheduleFullService.searchTourSchedule(filters, pageable));
    }
}


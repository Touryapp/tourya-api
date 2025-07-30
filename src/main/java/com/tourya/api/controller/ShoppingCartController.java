package com.tourya.api.controller;

import com.tourya.api.models.mapper.request.AddItemToCartRequest;
import com.tourya.api.models.mapper.responses.ShoppingCartResponse;
import com.tourya.api.services.ShoppingCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @PostMapping("/items")
    public ResponseEntity<ShoppingCartResponse> addItemToCart(
            @RequestBody @Valid AddItemToCartRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(shoppingCartService.addItemToCart(request, connectedUser));
    }

    @GetMapping
    public ResponseEntity<ShoppingCartResponse> getCartDetails(
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(shoppingCartService.getCartDetails(connectedUser));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable Long itemId,
            Authentication connectedUser
    ) {
        shoppingCartService.removeItemFromCart(itemId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<Void> checkout(
            Authentication connectedUser
    ) {
        shoppingCartService.checkout(connectedUser);
        return ResponseEntity.accepted().build();
    }
}

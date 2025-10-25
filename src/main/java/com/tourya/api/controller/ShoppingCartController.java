package com.tourya.api.controller;

import com.tourya.api.models.request.AddItemToCartRequest;
import com.tourya.api.models.request.AddMultipleItemsToCartRequest;
import com.tourya.api.models.responses.ClearCartResponse;
import com.tourya.api.models.responses.ShoppingCartResponse;
import com.tourya.api.models.request.CreateShoppingCartRequest;
import com.tourya.api.models.request.UpdateItemStatusRequest;
import com.tourya.api.services.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión del carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@RestController
@RequestMapping("/shopping-cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "API para la gestión del carrito de compras")
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @Operation(
            summary = "Crear carrito de compras",
            description = "Crea un nuevo carrito de compras para el usuario autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrito creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "409", description = "El usuario ya tiene un carrito activo")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ShoppingCartResponse> createShoppingCart(
            @RequestBody @Valid CreateShoppingCartRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(shoppingCartService.createShoppingCart(request, connectedUser));
    }

    @Operation(
            summary = "Agregar items al carrito",
            description = "Agrega uno o múltiples items al carrito de compras del usuario autenticado. Puede recibir un solo item o una lista de items."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Items agregados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Servicio o tour no encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/items")
    public ResponseEntity<ShoppingCartResponse> addItemsToCart(
            @RequestBody @Valid AddMultipleItemsToCartRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(shoppingCartService.addMultipleItemsToCart(request, connectedUser));
    }

    @Operation(
            summary = "Obtener todos los carritos",
            description = "Obtiene todos los carritos de compras con paginación"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carritos obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<ShoppingCartResponse>> getAllShoppingCarts(
            @Parameter(description = "Parámetros de paginación") Pageable pageable
    ) {
        return ResponseEntity.ok(shoppingCartService.getAllShoppingCarts(pageable));
    }

    @Operation(
            summary = "Obtener carrito activo del usuario",
            description = "Obtiene el carrito activo único del usuario autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrito activo obtenido exitosamente"),
            @ApiResponse(responseCode = "204", description = "No hay carrito activo"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/user")
    public ResponseEntity<ShoppingCartResponse> getActiveShoppingCartByUser(
            Authentication connectedUser
    ) {
        ShoppingCartResponse cart = shoppingCartService.getActiveShoppingCartByUser(connectedUser);
        if (cart != null) {
            return ResponseEntity.ok(cart);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @Operation(
            summary = "Obtener carrito por ID",
            description = "Obtiene un carrito de compras específico por su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrito obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{cartId}")
    public ResponseEntity<ShoppingCartResponse> getShoppingCartById(
            @Parameter(description = "ID del carrito") @PathVariable Long cartId
    ) {
        return ResponseEntity.ok(shoppingCartService.getShoppingCartById(cartId));
    }


    @Operation(
            summary = "Obtener detalles del carrito",
            description = "Obtiene todos los items de un carrito de compras específico por ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrito obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos para acceder a este carrito")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{cartId}/details")
    public ResponseEntity<ShoppingCartResponse> getCartDetails(
            @Parameter(description = "ID del carrito") @PathVariable Long cartId,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(shoppingCartService.getCartDetails(cartId, connectedUser));
    }

    @Operation(
            summary = "Eliminar item del carrito",
            description = "Elimina un item específico del carrito de compras"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Carrito o item no encontrado"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos para eliminar este item")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<Void> removeItemFromCart(
            @Parameter(description = "ID del carrito") @PathVariable Long cartId,
            @Parameter(description = "ID del item a eliminar") @PathVariable Long itemId,
            Authentication connectedUser
    ) {
        shoppingCartService.removeItemFromCart(cartId, itemId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Actualizar estado de item",
            description = "Actualiza el estado de un item específico del carrito de compras"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Carrito o item no encontrado"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos para actualizar este item")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{cartId}/items/{itemId}/status")
    public ResponseEntity<ShoppingCartResponse> updateItemStatus(
            @Parameter(description = "ID del carrito") @PathVariable Long cartId,
            @Parameter(description = "ID del item a actualizar") @PathVariable Long itemId,
            @RequestBody @Valid UpdateItemStatusRequest request,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(shoppingCartService.updateItemStatus(cartId, itemId, request, connectedUser));
    }

    @Operation(
            summary = "Procesar compra",
            description = "Procesa la compra de todos los items de un carrito específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Compra procesada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "400", description = "Carrito vacío o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado"),
            @ApiResponse(responseCode = "403", description = "No tienes permisos para procesar este carrito")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<Void> checkout(
            @Parameter(description = "ID del carrito a procesar") @PathVariable Long cartId,
            Authentication connectedUser
    ) {
        shoppingCartService.checkout(cartId, connectedUser);
        return ResponseEntity.accepted().build();
    }


    @DeleteMapping("/{cartId}/clear")
    public ResponseEntity<ClearCartResponse> clearCart(@PathVariable Long cartId) {
        ClearCartResponse response = shoppingCartService.clearShoppingCart(cartId);
        return ResponseEntity.ok(response);
    }
}
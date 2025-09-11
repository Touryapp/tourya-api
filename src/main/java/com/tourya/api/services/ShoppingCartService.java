package com.tourya.api.services;

import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.ShoppingCart;
import com.tourya.api.models.ShoppingCartItem;
import com.tourya.api.models.TourSchedule;
import com.tourya.api.models.TourScheduleConfig;
import com.tourya.api.models.TourScheduleConfigPrice;
import com.tourya.api.models.TourScheduleConfigSlot;
import com.tourya.api.models.User;
import com.tourya.api.models.mapper.request.AddItemToCartRequest;
import com.tourya.api.models.mapper.responses.ShoppingCartItemResponse;
import com.tourya.api.models.mapper.responses.ShoppingCartResponse;
import com.tourya.api.models.request.ReservationItemRequest;
import com.tourya.api.models.request.ReservationRequest;
import com.tourya.api.repository.ShoppingCartItemRepository;
import com.tourya.api.repository.ShoppingCartRepository;
import com.tourya.api.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourReservationService tourReservationService;

    @Transactional
    public ShoppingCartResponse addItemToCart(AddItemToCartRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        TourSchedule tourSchedule = tourScheduleRepository.findById(request.getTourScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour schedule not found"));

        // 1. Verificar disponibilidad
        if (!tourSchedule.getIsUnlimitedCapacity()) {
            int availableCapacity = tourSchedule.getMaxCapacity() - tourSchedule.getReservedCapacity();
            if (request.getQuantity() > availableCapacity) {
                throw new OperationNotPermittedException("Not enough capacity available for this tour schedule. Available: " + availableCapacity);
            }
        }

        // 2. Obtener el precio real basado en la edad
        TourScheduleConfigPrice tourScheduleConfigPrice = getPriceForTourSchedule(tourSchedule, request.getAge());
        BigDecimal unitPrice = tourScheduleConfigPrice.getPrice();

        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), ShoppingCartStatusEnum.ACTIVE)
                .orElseGet(() -> createNewCart(user));

        // Verificar si el item ya existe en el carrito para el mismo tourSchedule y priceConfig
        // Si existe, actualizar la cantidad y el precio total
        // Si no existe, añadir un nuevo item
        ShoppingCartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getTourSchedule().getId().equals(request.getTourScheduleId()) &&
                                 item.getTourScheduleConfigPrice().getId().equals(tourScheduleConfigPrice.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Actualizar cantidad y precios
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(existingItem.getQuantity())));
        } else {
            // Crear nuevo item
            ShoppingCartItem newItem = ShoppingCartItem.builder()
                    .shoppingCart(cart)
                    .tourSchedule(tourSchedule)
                    .tourScheduleConfigPrice(tourScheduleConfigPrice)
                    .tourScheduleConfigPriceId(tourScheduleConfigPrice.getId())
                    .quantity(request.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            cart.getItems().add(newItem);
        }

        shoppingCartRepository.save(cart);
        return buildShoppingCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public ShoppingCartResponse getCartDetails(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), ShoppingCartStatusEnum.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active shopping cart found for the user."));
        return buildShoppingCartResponse(cart);
    }

    @Transactional
    public void removeItemFromCart(Long itemId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        ShoppingCartItem item = shoppingCartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getShoppingCart().getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You are not allowed to remove this item.");
        }

        // Eliminar el item del carrito y actualizar el carrito
        item.getShoppingCart().getItems().remove(item);
        shoppingCartItemRepository.delete(item);
    }

    @Transactional
    public void checkout(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), ShoppingCartStatusEnum.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active shopping cart found to checkout."));

        if (cart.getItems().isEmpty()) {
            throw new OperationNotPermittedException("Cannot checkout an empty cart.");
        }

        // Construir ReservationRequest a partir de los items del carrito
        List<ReservationItemRequest> reservationItems = cart.getItems().stream()
                .map(item -> ReservationItemRequest.builder()
                        .priceId(item.getTourScheduleConfigPriceId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        ReservationRequest reservationRequest = ReservationRequest.builder()
                .scheduleId(cart.getItems().get(0).getTourSchedule().getId())
                .clientName(user.fullName())
                .clientEmail(user.getEmail())
                .clientPhone("N/A")
                .paymentMethod("CART_CHECKOUT")
                .currency("USD")
                .items(reservationItems)
                .build();

        tourReservationService.createReservation(reservationRequest, user);
        cart.setStatus(ShoppingCartStatusEnum.COMPLETED);
        shoppingCartRepository.save(cart);
    }

    private ShoppingCart createNewCart(User user) {
        return ShoppingCart.builder()
                .user(user)
                .status(ShoppingCartStatusEnum.ACTIVE)
                .items(new ArrayList<>())
                .build();
    }

    private TourScheduleConfigPrice getPriceForTourSchedule(TourSchedule tourSchedule, Integer age) {
        TourScheduleConfig tourScheduleConfig = tourSchedule.getConfig();
        if (tourScheduleConfig == null) {
            throw new ResourceNotFoundException("Tour schedule configuration not found for schedule ID: " + tourSchedule.getId());
        }

        TourScheduleConfigSlot matchingSlot = tourScheduleConfig.getSlots().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No matching schedule slot found for tour schedule ID: " + tourSchedule.getId()));

        return matchingSlot.getPrices().stream()
                .filter(price -> age >= price.getMinAge() && age <= price.getMaxAge())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No price found for age " + age + " in schedule slot ID: " + matchingSlot.getId()));
    }

    private ShoppingCartResponse buildShoppingCartResponse(ShoppingCart cart) {
        List<ShoppingCartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> ShoppingCartItemResponse.builder()
                        .id(item.getId())
                        .tourScheduleId(item.getTourSchedule().getId())
                        .tourName(item.getTourSchedule().getTour().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .ageType(item.getTourScheduleConfigPrice() != null ? item.getTourScheduleConfigPrice().getAgeType() : null)
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalAmount = itemResponses.stream()
                .map(ShoppingCartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ShoppingCartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .status(cart.getStatus())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .build();
    }
}
package com.tourya.api.services;

import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.request.AddItemToCartRequest;
import com.tourya.api.models.request.AddMultipleItemsToCartRequest;
import com.tourya.api.models.request.ConfigQuantityRequest;
import com.tourya.api.models.responses.ShoppingCartItemDetailResponse;
import com.tourya.api.models.responses.ShoppingCartItemResponse;
import com.tourya.api.models.responses.ShoppingCartResponse;
import com.tourya.api.models.request.CreateShoppingCartRequest;
import com.tourya.api.models.request.ReservationItemRequest;
import com.tourya.api.models.request.ReservationRequest;
import com.tourya.api.models.request.UpdateItemStatusRequest;
import com.tourya.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión del carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;
    private final ServiceRepository serviceRepository;
    private final TourReservationService tourReservationService;

    /**
     * Crea un nuevo carrito de compras para un usuario.
     * 
     * @param request datos para crear el carrito
     * @param connectedUser usuario autenticado
     * @return ShoppingCartResponse con los datos del carrito creado
     */
    @Transactional
    public ShoppingCartResponse createShoppingCart(CreateShoppingCartRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Crear nuevo carrito (sin verificar carritos existentes)
        ShoppingCart cart = ShoppingCart.builder()
                .user(user)
                .status(ShoppingCartStatusEnum.ACTIVE)
                .items(new ArrayList<>())
                .build();

        cart = shoppingCartRepository.save(cart);
        return buildShoppingCartResponse(cart);
    }

    /**
     * Agrega un item al carrito de compras.
     * 
     * @param request datos del item a agregar
     * @param connectedUser usuario autenticado
     * @return ShoppingCartResponse con los datos actualizados del carrito
     */
    @Transactional
    public ShoppingCartResponse addItemToCart(AddItemToCartRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Verificar que el servicio existe (solo si se proporciona serviceId)
        TouryaService service = null;
        if (request.getServiceId() != null) {
            service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        }

        // Buscar carrito existente o crear uno nuevo
        ShoppingCart cart;
        if (request.getCartId() != null) {
            // Buscar carrito existente por ID
            cart = shoppingCartRepository.findById(request.getCartId())
                    .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        } else {
            // Crear nuevo carrito
            cart = createNewCart(user);
        }

        // Verificar disponibilidad para tours
        if (request.getTourScheduleId() != null) {
        TourSchedule tourSchedule = tourScheduleRepository.findById(request.getTourScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horario de tour no encontrado"));

        if (!tourSchedule.getIsUnlimitedCapacity()) {
            int availableCapacity = tourSchedule.getMaxCapacity() - tourSchedule.getReservedCapacity();
                // Calcular cantidad total desde los details
                int totalQuantity = 0;
                if (request.getSlot() != null && request.getSlot().getConfigQuantity() != null) {
                    totalQuantity = request.getSlot().getConfigQuantity().stream()
                            .mapToInt(ConfigQuantityRequest::getQuantity)
                            .sum();
                }
                if (totalQuantity > availableCapacity) {
                    throw new OperationNotPermittedException("No hay suficiente capacidad disponible. Disponible: " + availableCapacity);
                }
            }
        }

        // Calcular total price desde los details
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Crear nuevo item
        ShoppingCartItem newItem = ShoppingCartItem.builder()
                .shoppingCart(cart)
                .productId(request.getProductId())
                .productType(request.getProductType())
                .scheduleDate(request.getScheduleDate())
                .tourSchedule(request.getTourScheduleId() != null ? 
                    tourScheduleRepository.findById(request.getTourScheduleId()).orElse(null) : null)
                .slot(request.getSlot() != null && request.getSlot().getId() != null ?
                    tourScheduleConfigSlotRepository.findById(request.getSlot().getId().intValue()).orElse(null) : null)
                .totalPrice(totalPrice)
                .status(ShoppingCartStatusEnum.ACTIVE)
                .details(new ArrayList<>())
                .build();

        // Crear details para cada ageType
        if (request.getSlot() != null && request.getSlot().getConfigQuantity() != null) {
            TourScheduleConfigSlot slot = tourScheduleConfigSlotRepository.findById(request.getSlot().getId().intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));
            
            for (ConfigQuantityRequest configQuantity : request.getSlot().getConfigQuantity()) {
                // Buscar precio por ageType
                AgePriceType ageType = AgePriceType.valueOf(configQuantity.getAgeType());
                BigDecimal unitPrice = slot.getPrices().stream()
                        .filter(price -> price.getAgeType().equals(ageType))
                .findFirst()
                        .map(TourScheduleConfigPrice::getPrice)
                        .orElse(BigDecimal.ZERO);
                
                BigDecimal detailTotalPrice = unitPrice.multiply(BigDecimal.valueOf(configQuantity.getQuantity()));
                totalPrice = totalPrice.add(detailTotalPrice);
                
                // Crear detail
                ShoppingCartItemDetail detail = ShoppingCartItemDetail.builder()
                        .shoppingCartItem(newItem)
                        .ageType(ageType)
                        .quantity(configQuantity.getQuantity())
                        .unitPrice(unitPrice)
                        .totalPrice(detailTotalPrice)
                        .build();
                
                newItem.getDetails().add(detail);
            }
        }
        
        // Actualizar total price del item
        newItem.setTotalPrice(totalPrice);

        cart.getItems().add(newItem);
        shoppingCartRepository.save(cart);
        
        return buildShoppingCartResponse(cart);
    }

    /**
     * Agrega múltiples items al carrito de compras.
     * 
     * @param request lista de items a agregar
     * @param connectedUser usuario autenticado
     * @return ShoppingCartResponse con los datos del carrito actualizado
     */
    @Transactional
    public ShoppingCartResponse addMultipleItemsToCart(AddMultipleItemsToCartRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Determinar el carrito a usar
        ShoppingCart cart;
        if (request.getCartId() != null) {
            // Buscar carrito existente por ID
            cart = shoppingCartRepository.findById(request.getCartId())
                    .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        } else {
            // Crear nuevo carrito
            cart = createNewCart(user);
        }

        // Agregar cada item al carrito
        for (AddItemToCartRequest itemRequest : request.getItems()) {
            addItemToExistingCart(itemRequest, cart);
        }
        
        // Guardar el carrito actualizado
        cart = shoppingCartRepository.save(cart);
        
        return buildShoppingCartResponse(cart);
    }

    /**
     * Agrega un item a un carrito existente.
     * 
     * @param request datos del item a agregar
     * @param cart carrito existente
     */
    private void addItemToExistingCart(AddItemToCartRequest request, ShoppingCart cart) {
        // Validar que el servicio existe si se proporciona
        if (request.getServiceId() != null) {
            TouryaService service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        }

        // Validar capacidad del slot si se proporciona
        if (request.getSlot() != null && request.getSlot().getConfigQuantity() != null) {
            TourScheduleConfigSlot slot = tourScheduleConfigSlotRepository.findById(request.getSlot().getId().intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));

            // Calcular cantidad total
            int totalQuantity = request.getSlot().getConfigQuantity().stream()
                    .mapToInt(ConfigQuantityRequest::getQuantity)
                    .sum();

            // Verificar capacidad
            if (slot.getMaxCapacity() != null && totalQuantity > slot.getMaxCapacity()) {
                throw new OperationNotPermittedException("La cantidad solicitada excede la capacidad máxima del slot");
            }
        }

        // Calcular total price desde los details
        BigDecimal totalPrice = BigDecimal.ZERO;

            // Crear nuevo item
            ShoppingCartItem newItem = ShoppingCartItem.builder()
                    .shoppingCart(cart)
                .productId(request.getProductId())
                .productType(request.getProductType())
                .scheduleDate(request.getScheduleDate())
                .tourSchedule(request.getTourScheduleId() != null ? 
                    tourScheduleRepository.findById(request.getTourScheduleId()).orElse(null) : null)
                .slot(request.getSlot() != null && request.getSlot().getId() != null ?
                    tourScheduleConfigSlotRepository.findById(request.getSlot().getId().intValue()).orElse(null) : null)
                .totalPrice(totalPrice)
                .status(ShoppingCartStatusEnum.ACTIVE)
                .details(new ArrayList<>())
                .build();

        // Crear details para cada ageType
        if (request.getSlot() != null && request.getSlot().getConfigQuantity() != null) {
            TourScheduleConfigSlot slot = tourScheduleConfigSlotRepository.findById(request.getSlot().getId().intValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));

            for (ConfigQuantityRequest configQuantity : request.getSlot().getConfigQuantity()) {
                AgePriceType ageType = AgePriceType.valueOf(configQuantity.getAgeType());
                
                // Buscar precio en el slot
                BigDecimal unitPrice = slot.getPrices().stream()
                        .filter(price -> price.getAgeType().equals(ageType))
                        .findFirst()
                        .map(TourScheduleConfigPrice::getPrice)
                        .orElse(BigDecimal.ZERO);

                BigDecimal detailTotalPrice = unitPrice.multiply(BigDecimal.valueOf(configQuantity.getQuantity()));
                totalPrice = totalPrice.add(detailTotalPrice);

                ShoppingCartItemDetail detail = ShoppingCartItemDetail.builder()
                        .shoppingCartItem(newItem)
                        .ageType(ageType)
                        .quantity(configQuantity.getQuantity())
                    .unitPrice(unitPrice)
                        .totalPrice(detailTotalPrice)
                    .build();

                newItem.getDetails().add(detail);
            }
        }

        // Actualizar precio total del item
        newItem.setTotalPrice(totalPrice);

        // Agregar item al carrito
            cart.getItems().add(newItem);
        }

    /**
     * Obtiene todos los carritos de compras con paginación.
     * 
     * @param pageable parámetros de paginación
     * @return Page con los carritos de compras
     */
    @Transactional(readOnly = true)
    public Page<ShoppingCartResponse> getAllShoppingCarts(Pageable pageable) {
        Page<ShoppingCart> carts = shoppingCartRepository.findAll(pageable);
        return carts.map(this::buildShoppingCartResponse);
    }

    /**
     * Obtiene un carrito de compras por su ID.
     * 
     * @param cartId ID del carrito
     * @return ShoppingCartResponse con los datos del carrito
     */
    @Transactional(readOnly = true)
    public ShoppingCartResponse getShoppingCartById(Long cartId) {
        ShoppingCart cart = shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        return buildShoppingCartResponse(cart);
    }

    /**
     * Obtiene los carritos de compras de un usuario ordenados por tour y fecha de viaje.
     * 
     * @param connectedUser usuario autenticado
     * @return Lista de carritos del usuario
     */
    @Transactional(readOnly = true)
    public List<ShoppingCartResponse> getShoppingCartsByUser(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        List<ShoppingCart> carts = shoppingCartRepository.findByUserIdOrderByCreatedDateDesc(user.getId());
        return carts.stream()
                .map(this::buildShoppingCartResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los detalles de un carrito de compras por ID.
     * 
     * @param cartId ID del carrito
     * @param connectedUser usuario autenticado
     * @return ShoppingCartResponse con los datos del carrito
     */
    @Transactional(readOnly = true)
    public ShoppingCartResponse getCartDetails(Long cartId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        ShoppingCart cart = shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        
        // Verificar que el carrito pertenece al usuario
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("No tienes permisos para acceder a este carrito");
        }
        
        return buildShoppingCartResponse(cart);
    }

    /**
     * Elimina un item del carrito de compras.
     * 
     * @param cartId ID del carrito
     * @param itemId ID del item a eliminar
     * @param connectedUser usuario autenticado
     */
    @Transactional
    public void removeItemFromCart(Long cartId, Long itemId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Verificar que el carrito existe y pertenece al usuario
        ShoppingCart cart = shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("No tienes permisos para acceder a este carrito");
        }
        
        // Buscar el item en el carrito
        ShoppingCartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado en el carrito"));

        cart.getItems().remove(item);
        shoppingCartItemRepository.delete(item);
    }

    /**
     * Actualiza el estado de un item del carrito.
     * 
     * @param cartId ID del carrito
     * @param itemId ID del item
     * @param request nuevo estado
     * @param connectedUser usuario autenticado
     * @return ShoppingCartResponse con los datos actualizados del carrito
     */
    @Transactional
    public ShoppingCartResponse updateItemStatus(Long cartId, Long itemId, UpdateItemStatusRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Verificar que el carrito existe y pertenece al usuario
        ShoppingCart cart = shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("No tienes permisos para acceder a este carrito");
        }
        
        // Buscar el item en el carrito
        ShoppingCartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado en el carrito"));

        item.setStatus(request.getStatus());
        shoppingCartItemRepository.save(item);
        
        return buildShoppingCartResponse(cart);
    }

    /**
     * Procesa el checkout del carrito de compras.
     * 
     * @param cartId ID del carrito a procesar
     * @param connectedUser usuario autenticado
     */
    @Transactional
    public void checkout(Long cartId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        ShoppingCart cart = shoppingCartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
        
        // Verificar que el carrito pertenece al usuario
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("No tienes permisos para procesar este carrito");
        }

        if (cart.getItems().isEmpty()) {
            throw new OperationNotPermittedException("No se puede procesar un carrito vacío");
        }

        // Procesar solo items de tours para la reserva
        List<ShoppingCartItem> tourItems = cart.getItems().stream()
                .filter(item -> item.getTourSchedule() != null)
                .collect(Collectors.toList());

        if (!tourItems.isEmpty()) {
        // Construir ReservationRequest a partir de los items del carrito
            List<ReservationItemRequest> reservationItems = tourItems.stream()
                    .map(item -> {
                        // Calcular cantidad total desde los details
                        int totalQuantity = item.getDetails().stream()
                                .mapToInt(ShoppingCartItemDetail::getQuantity)
                                .sum();
                        
                        return ReservationItemRequest.builder()
                                .priceId(item.getProductId()) // Usar productId como priceId temporalmente
                                .quantity(totalQuantity)
                                .build();
                    })
                .collect(Collectors.toList());

        ReservationRequest reservationRequest = ReservationRequest.builder()
                    .scheduleId(tourItems.get(0).getTourSchedule().getId())
                .clientName(user.fullName())
                .clientEmail(user.getEmail())
                .clientPhone("N/A")
                .paymentMethod("CART_CHECKOUT")
                .currency("USD")
                .items(reservationItems)
                .build();

        tourReservationService.createReservation(reservationRequest, user);
        }

        // Marcar todos los items como completados
        cart.getItems().forEach(item -> item.setStatus(ShoppingCartStatusEnum.COMPLETED));
        cart.setStatus(ShoppingCartStatusEnum.COMPLETED);
        shoppingCartRepository.save(cart);
    }

    /**
     * Crea un nuevo carrito para un usuario.
     * 
     * @param user usuario para el cual crear el carrito
     * @return ShoppingCart creado
     */
    private ShoppingCart createNewCart(User user) {
        ShoppingCart newCart = ShoppingCart.builder()
                .user(user)
                .status(ShoppingCartStatusEnum.ACTIVE)
                .items(new ArrayList<>())
                .build();
        
        // Guardar el carrito en la base de datos para obtener el ID
        return shoppingCartRepository.save(newCart);
    }

    /**
     * Construye la respuesta del carrito de compras.
     * 
     * @param cart carrito de compras
     * @return ShoppingCartResponse
     */
    private ShoppingCartResponse buildShoppingCartResponse(ShoppingCart cart) {
        List<ShoppingCartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    // Mapear detalles del item
                    List<ShoppingCartItemDetailResponse> detailResponses = item.getDetails().stream()
                            .map(detail -> ShoppingCartItemDetailResponse.builder()
                                    .id(detail.getId())
                                    .ageType(detail.getAgeType())
                                    .quantity(detail.getQuantity())
                                    .unitPrice(detail.getUnitPrice())
                                    .totalPrice(detail.getTotalPrice())
                        .build())
                            .collect(Collectors.toList());

                    return ShoppingCartItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productType(item.getProductType())
                            .serviceId(null) // service_id se maneja a través de product_id y product_type
                            .serviceName(null)
                            .serviceType(null)
                            .scheduleDate(item.getScheduleDate())
                            .tourScheduleId(item.getTourSchedule() != null ? item.getTourSchedule().getId() : null)
                            .tourName(item.getTourSchedule() != null ? 
                                item.getTourSchedule().getTour().getName() : null)
                            .slotId(item.getSlot() != null ? item.getSlot().getId() : null)
                            .totalPrice(item.getTotalPrice())
                            .status(item.getStatus())
                            .details(detailResponses)
                            .build();
                })
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
                .creationDate(cart.getCreatedDate())
                .lastModifiedDate(cart.getLastModifiedDate())
                .build();
    }
}
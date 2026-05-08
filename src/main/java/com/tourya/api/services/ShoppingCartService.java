package com.tourya.api.services;

import com.tourya.api.constans.enums.AgePriceType;
import com.tourya.api.constans.enums.ShoppingCartStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.*;
import com.tourya.api.models.request.AddItemToCartRequest;
import com.tourya.api.models.request.AddMultipleItemsToCartRequest;
import com.tourya.api.models.request.ConfigQuantityRequest;
import com.tourya.api.models.responses.ClearCartResponse;
import com.tourya.api.models.responses.ShoppingCartItemDetailResponse;
import com.tourya.api.models.responses.ShoppingCartItemResponse;
import com.tourya.api.models.responses.ShoppingCartResponse;
import com.tourya.api.models.request.CreateShoppingCartRequest;
import com.tourya.api.models.request.ReservationItemRequest;
import com.tourya.api.models.request.ReservationRequest;
import com.tourya.api.models.request.SlotRequest;
import com.tourya.api.models.request.UpdateItemStatusRequest;
import com.tourya.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión del carrito de compras.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService {
    private static final ZoneId CO_ZONE = ZoneId.of("America/Bogota");

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final EntityManager entityManager;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourScheduleConfigSlotRepository tourScheduleConfigSlotRepository;
    private final TourRepository tourRepository;
    private final ServiceRepository serviceRepository;
    private final TourReservationService tourReservationService;
    private final AgeRangeConfigService ageRangeConfigService;
    private final TourScheduleSlotAvailabilityService tourScheduleSlotAvailabilityService;

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
        
        // Buscar carrito activo existente o crear uno nuevo
        ShoppingCart cart = findOrCreateActiveCart(user);
        return buildShoppingCartResponse(cart);
    }

    /**
     * Busca el carrito activo del usuario o crea uno nuevo si no existe.
     * Implementa la lógica de carrito activo único por usuario.
     * 
     * @param user usuario para el cual buscar/crear el carrito
     * @return carrito activo del usuario
     */
    private ShoppingCart findOrCreateActiveCart(User user) {
        // Buscar carrito activo del usuario
        List<ShoppingCart> activeCarts = shoppingCartRepository.findByUserAndStatus(user, ShoppingCartStatusEnum.ACTIVE);
        
        if (!activeCarts.isEmpty()) {
            // Si existe un carrito activo, devolverlo
            return activeCarts.get(0);
        } else {
            // Si no existe carrito activo, crear uno nuevo
            return createNewCart(user);
        }
    }

    /**
     * Crea un nuevo carrito de compras para un usuario.
     * 
     * @param user usuario para el cual crear el carrito
     * @return carrito creado
     */
    private ShoppingCart createNewCart(User user) {
        ShoppingCart cart = ShoppingCart.builder()
                .user(user)
                .status(ShoppingCartStatusEnum.ACTIVE)
                .items(new ArrayList<>())
                .build();

        return shoppingCartRepository.save(cart);
    }

    /**
     * Capacidad solo a nivel de slot ({@link TourScheduleConfigSlot}) y tour ilimitado ({@link Tour}).
     */
    private void validateTourCapacityForCartRequest(TourSchedule tourSchedule, Tour tour, SlotRequest slotRequest) {
        if (tour != null && Boolean.TRUE.equals(tour.getIsUnlimitedCapacity())) {
            return;
        }
        if (tour == null) {
            throw new ResourceNotFoundException("Tour no encontrado para el horario indicado");
        }
        if (slotRequest == null || slotRequest.getId() == null) {
            throw new OperationNotPermittedException("Debe indicar el slot para validar capacidad");
        }
        int totalPax = slotRequest.getConfigQuantity() != null
                ? slotRequest.getConfigQuantity().stream().mapToInt(ConfigQuantityRequest::getQuantity).sum()
                : 0;
        TourScheduleConfigSlot slot = tourScheduleConfigSlotRepository.findById(slotRequest.getId().intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));
        tourScheduleSlotAvailabilityService.ensureSlotHasCapacity(tour, slot, totalPax);
    }

    /**
     * Agrega un item al carrito de compras.
     * Implementa la lógica de carrito activo único por usuario.
     * 
     * @param request datos del item a agregar
     * @param connectedUser usuario autenticado
     * @return ShoppingCartResponse con los datos actualizados del carrito
     */
    @Transactional
    public ShoppingCartResponse addItemToCart(AddItemToCartRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Validar que la fecha del schedule no sea anterior a la fecha actual
        if (request.getScheduleDate() != null && request.getScheduleDate().isBefore(LocalDate.now(CO_ZONE))) {
            throw new OperationNotPermittedException("No se puede agregar un item al carrito con una fecha anterior a la fecha actual");
        }
        
        // Verificar que el servicio existe (solo si se proporciona serviceId)
        TouryaService service = null;
        if (request.getServiceId() != null) {
            service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        }

        // Buscar carrito activo del usuario o crear uno nuevo
        ShoppingCart cart = findOrCreateActiveCart(user);

        if (request.getTourScheduleId() != null) {
            TourSchedule tourSchedule = tourScheduleRepository.findById(request.getTourScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horario de tour no encontrado"));
            Tour tour = tourSchedule.getTourId() != null
                    ? tourRepository.findById(tourSchedule.getTourId()).orElse(null)
                    : null;
            validateTourCapacityForCartRequest(tourSchedule, tour, request.getSlot());
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
            
            // Obtener el Tour para acceder a priceType
            Tour tour = null;
            if (request.getTourScheduleId() != null) {
                TourSchedule schedule = tourScheduleRepository.findById(request.getTourScheduleId()).orElse(null);
                if (schedule != null && schedule.getTourId() != null) {
                    tour = tourRepository.findById(schedule.getTourId()).orElse(null);
                }
            }
            
            for (ConfigQuantityRequest configQuantity : request.getSlot().getConfigQuantity()) {
                // Buscar precio por ageType
                AgePriceType ageType = AgePriceType.valueOf(configQuantity.getAgeType());
                TourScheduleConfigPrice priceConfig = slot.getPrices().stream()
                        .filter(price -> price.getAgeType().equals(ageType))
                        .findFirst()
                        .orElse(null);
                
                if (priceConfig == null) {
                    throw new ResourceNotFoundException("Precio no encontrado para ageType: " + ageType);
                }
                
                // Obtener configuración de rango de edad desde age_range_config
                AgeRangeConfig ageRangeConfig = ageRangeConfigService.getByAgeType(ageType);
                
                BigDecimal unitPrice = priceConfig.getPrice();
                BigDecimal providerUnitPrice = priceConfig.getProviderPrice() != null 
                        ? priceConfig.getProviderPrice() 
                        : BigDecimal.ZERO;
                
                // Calcular precio total según priceType del tour
                BigDecimal detailTotalPrice;
                BigDecimal providerTotalPrice;
                Integer quantity = configQuantity.getQuantity();
                
                if (tour != null && tour.getPriceType() != null 
                        && tour.getPriceType().getValue().equals("grupo")) {
                    // Para tours GRUPO: el precio es fijo independientemente de la cantidad
                    detailTotalPrice = unitPrice;
                    providerTotalPrice = providerUnitPrice;
                } else {
                    // Para tours INDIVIDUAL: precio por persona
                    detailTotalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
                    providerTotalPrice = providerUnitPrice.multiply(BigDecimal.valueOf(quantity));
                }
                
                totalPrice = totalPrice.add(detailTotalPrice);
                
                // Crear detail
                ShoppingCartItemDetail detail = ShoppingCartItemDetail.builder()
                        .shoppingCartItem(newItem)
                        .ageType(ageType)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .providerUnitPrice(providerUnitPrice)
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
     * Implementa la lógica de carrito activo único por usuario.
     * 
     * @param request lista de items a agregar
     * @param connectedUser usuario autenticado
     * @return ShoppingCartResponse con los datos del carrito actualizado
     */
    @Transactional
    public ShoppingCartResponse addMultipleItemsToCart(AddMultipleItemsToCartRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Usar el carrito activo del usuario o crear uno nuevo
        ShoppingCart cart = findOrCreateActiveCart(user);
        
        // Asegurarse de que los items se carguen inicializando la colección
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        } else {
            // Forzar la carga de items si están en lazy loading
            cart.getItems().size();
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
        // Validar que la fecha del schedule no sea anterior a la fecha actual
        if (request.getScheduleDate() != null && request.getScheduleDate().isBefore(LocalDate.now(CO_ZONE))) {
            throw new OperationNotPermittedException("No se puede agregar un item al carrito con una fecha anterior a la fecha actual");
        }
        
        // Validar que el servicio existe si se proporciona
        if (request.getServiceId() != null) {
            TouryaService service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        }

        if (request.getTourScheduleId() != null) {
            TourSchedule tourSchedule = tourScheduleRepository.findById(request.getTourScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Horario de tour no encontrado"));
            Tour tour = tourSchedule.getTourId() != null
                    ? tourRepository.findById(tourSchedule.getTourId()).orElse(null)
                    : null;
            validateTourCapacityForCartRequest(tourSchedule, tour, request.getSlot());
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

            // Obtener el Tour para acceder a priceType
            Tour tour = null;
            if (request.getTourScheduleId() != null) {
                TourSchedule schedule = tourScheduleRepository.findById(request.getTourScheduleId()).orElse(null);
                if (schedule != null && schedule.getTourId() != null) {
                    tour = tourRepository.findById(schedule.getTourId()).orElse(null);
                }
            }
            
            for (ConfigQuantityRequest configQuantity : request.getSlot().getConfigQuantity()) {
                AgePriceType ageType = AgePriceType.valueOf(configQuantity.getAgeType());
                
                // Buscar precio en el slot
                TourScheduleConfigPrice priceConfig = slot.getPrices().stream()
                        .filter(price -> price.getAgeType().equals(ageType))
                        .findFirst()
                        .orElse(null);
                
                if (priceConfig == null) {
                    throw new ResourceNotFoundException("Precio no encontrado para ageType: " + ageType);
                }
                
                // Obtener configuración de rango de edad desde age_range_config
                // Esto permite acceder a minAge y maxAge para validaciones futuras
                AgeRangeConfig ageRangeConfig = ageRangeConfigService.getByAgeType(ageType);
                
                BigDecimal unitPrice = priceConfig.getPrice();
                BigDecimal providerUnitPrice = priceConfig.getProviderPrice() != null 
                        ? priceConfig.getProviderPrice() 
                        : BigDecimal.ZERO;
                
                // Calcular precio total según priceType del tour
                BigDecimal detailTotalPrice;
                Integer quantity = configQuantity.getQuantity();
                
                if (tour != null && tour.getPriceType() != null 
                        && tour.getPriceType().getValue().equals("grupo")) {
                    // Para tours GRUPO: el precio es fijo independientemente de la cantidad
                    detailTotalPrice = unitPrice;
                } else {
                    // Para tours INDIVIDUAL: precio por persona
                    detailTotalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
                }
                
                totalPrice = totalPrice.add(detailTotalPrice);

                ShoppingCartItemDetail detail = ShoppingCartItemDetail.builder()
                        .shoppingCartItem(newItem)
                        .ageType(ageType)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .providerUnitPrice(providerUnitPrice)
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
     * Obtiene el carrito activo de un usuario.
     * Implementa la lógica de carrito activo único por usuario.
     * 
     * @param connectedUser usuario autenticado
     * @return Carrito activo del usuario (puede ser null si no existe)
     */
    @Transactional(readOnly = true)
    public ShoppingCartResponse getActiveShoppingCartByUser(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        
        // Buscar carrito activo del usuario
        List<ShoppingCart> activeCarts = shoppingCartRepository.findByUserAndStatus(user, ShoppingCartStatusEnum.ACTIVE);
        
        if (!activeCarts.isEmpty()) {
            return buildShoppingCartResponse(activeCarts.get(0));
        } else {
            return null; // No hay carrito activo
        }
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
     * Verifica si un carrito debe ser desactivado cuando todos sus items están inactivos.
     * 
     * @param cart carrito a verificar
     */
    private void checkAndDeactivateCartIfNeeded(ShoppingCart cart) {
        List<ShoppingCartItem> allItems = shoppingCartItemRepository.findByShoppingCart(cart);
        
        if (!allItems.isEmpty()) {
            // Verificar si todos los items están inactivos (PAID, COMPLETED, ABANDONED)
            boolean allItemsInactive = allItems.stream()
                    .allMatch(item -> item.getStatus() == ShoppingCartStatusEnum.PAID || 
                                    item.getStatus() == ShoppingCartStatusEnum.COMPLETED ||
                                    item.getStatus() == ShoppingCartStatusEnum.ABANDONED);

            if (allItemsInactive) {
                // Desactivar el carrito
                cart.setStatus(ShoppingCartStatusEnum.COMPLETED);
                shoppingCartRepository.save(cart);
                // Log para auditoría
                System.out.println("Cart " + cart.getId() + " deactivated - all items are inactive");
            }
        }
    }

    /**
     * Construye la respuesta del carrito de compras.
     * 
     * @param cart carrito de compras
     * @return ShoppingCartResponse
     */
    private ShoppingCartResponse buildShoppingCartResponse(ShoppingCart cart) {
        return buildShoppingCartResponse(cart, null);
    }

    /**
     * Construye la respuesta del carrito de compras, opcionalmente filtrando por status.
     * 
     * @param cart carrito de compras
     * @param statusFilter status para filtrar items (null = sin filtro)
     * @return ShoppingCartResponse
     */
    private ShoppingCartResponse buildShoppingCartResponse(ShoppingCart cart, ShoppingCartStatusEnum statusFilter) {
        List<ShoppingCartItemResponse> itemResponses = cart.getItems().stream()
                .filter(item -> statusFilter == null || item.getStatus() == statusFilter)
                .map(item -> {
                    // Mapear detalles del item
                    List<ShoppingCartItemDetailResponse> detailResponses = item.getDetails().stream()
                            .map(detail -> ShoppingCartItemDetailResponse.builder()
                                    .id(detail.getId())
                                    .ageType(detail.getAgeType())
                                    .quantity(detail.getQuantity())
                                    .unitPrice(detail.getUnitPrice())
                                    .providerUnitPrice(detail.getProviderUnitPrice())
                                    .totalPrice(detail.getTotalPrice())
                        .build())
                            .collect(Collectors.toList());

                    // Obtener productName y tourName según el tipo de producto
                    String productName = null;
                    Integer tourScheduleId = null;
                    String tourName = null;
                    
                    if ("SERVICE".equalsIgnoreCase(item.getProductType())) {
                        // Cuando es SERVICE, obtener el nombre del servicio
                        TouryaService service = serviceRepository.findById(item.getProductId()).orElse(null);
                        if (service != null) {
                            productName = service.getName();
                        }
                    } else if ("TOUR".equalsIgnoreCase(item.getProductType())) {
                        // Cuando es TOUR, obtener productName y tourName
                        if (item.getTourSchedule() != null) {
                            tourScheduleId = item.getTourSchedule().getId();
                            if (item.getTourSchedule().getTour() != null) {
                                if (item.getTourSchedule().getTour().getName() != null) {
                                    productName = item.getTourSchedule().getTour().getName().getEs();
                                    tourName = productName; // tourName es igual a productName para TOUR
                                }
                            }
                        }
                    }

                    return ShoppingCartItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productType(item.getProductType())
                            .productName(productName)
                            .scheduleDate(item.getScheduleDate())
                            .tourScheduleId(tourScheduleId)
                            .tourName(tourName)
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

    @Transactional
    public ClearCartResponse clearShoppingCart(Long cartId) {
        Integer deletedCount = shoppingCartRepository.clearCart(cartId);
        return new ClearCartResponse(deletedCount != null ? deletedCount : 0);
    }

    /**
     * Elimina FÍSICAMENTE todos los items ACTIVE del carrito activo del usuario.
     * Cambia el estado del carrito a COMPLETED para que se pueda crear un nuevo carrito ACTIVE.
     * 
     * @param user usuario cuyo carrito activo se debe limpiar
     * @return número de items ACTIVE eliminados
     */
    @Transactional
    public int removeActiveItemsFromUserCart(User user) {
        List<ShoppingCart> activeCarts = shoppingCartRepository.findByUserAndStatus(user, ShoppingCartStatusEnum.ACTIVE);
        int totalDeleted = 0;
        
        for (ShoppingCart cart : activeCarts) {
            Long cartId = cart.getId();
            
            log.info("Removing ACTIVE items from cart {} for user {}", cartId, user.getId());
            
            // 1. DELETE DIRECTO de los detalles de items ACTIVE
            shoppingCartItemRepository.deleteActiveItemDetailsByCartId(cartId);
            
            // 2. DELETE DIRECTO de todos los items ACTIVE
            int deleted = shoppingCartItemRepository.deleteActiveItemsByCartId(cartId);
            totalDeleted += deleted;
            
            log.info("Deleted {} ACTIVE items from cart {}", deleted, cartId);
            
            // 3. CAMBIAR el estado del carrito a COMPLETED
            // Usar update directo para asegurar que se aplique
            shoppingCartRepository.findById(cartId).ifPresent(cartToUpdate -> {
                cartToUpdate.setStatus(ShoppingCartStatusEnum.COMPLETED);
                shoppingCartRepository.saveAndFlush(cartToUpdate);
                log.info("Changed cart {} status to COMPLETED", cartId);
            });
        }
        
        // Forzar flush para que los cambios se reflejen inmediatamente
        entityManager.flush();
        entityManager.clear();
        
        log.info("Removed {} ACTIVE items from active carts for user {} and changed {} carts to COMPLETED", 
                totalDeleted, user.getId(), activeCarts.size());
        
        return totalDeleted;
    }
}
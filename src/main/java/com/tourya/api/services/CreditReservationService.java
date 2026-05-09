package com.tourya.api.services;

import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.Credit;
import com.tourya.api.models.User;
import com.tourya.api.models.request.ReserveCreditRequest;
import com.tourya.api.models.responses.ReserveCreditResponse;
import com.tourya.api.repository.CreditRepository;
import com.tourya.api.repository.ShoppingCartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio intermedio para reservar créditos asociados a un item del carrito.
 * Distribuye el monto entre los créditos indicados (mayor a menor) y los marca como RESERVED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreditReservationService {

    private final CreditRepository creditRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;

    /**
     * Reserva créditos para un item del carrito.
     * Los créditos se consumen en orden de mayor monto hasta cubrir amountToReserve.
     * Se guarda reserved_amount en cada crédito y se asocia al shopping_cart_item_id.
     *
     * @param request  shoppingCartItemId, amountToReserve, creditIds
     * @param authentication usuario autenticado (los créditos deben ser del usuario)
     * @return ReserveCreditResponse con el monto reservado y los ids de créditos usados
     */
    public ReserveCreditResponse reserveCredits(ReserveCreditRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Integer userId = user.getId();

        if (request.getAmountToReserve() == null || request.getAmountToReserve().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a reservar debe ser mayor a 0");
        }
        if (request.getCreditIds() == null || request.getCreditIds().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un ID de crédito");
        }

        // Validar que el item existe y pertenece al usuario del token
        var item = shoppingCartItemRepository.findById(request.getShoppingCartItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item del carrito no encontrado: " + request.getShoppingCartItemId()));
        if (!item.getShoppingCart().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("El item del carrito debe corresponder al usuario autenticado (token)");
        }

        Set<Long> creditIdSet = new HashSet<>(request.getCreditIds());
        List<Credit> credits = creditRepository.findByIdInAndUserId(creditIdSet, userId);
        if (credits.size() != creditIdSet.size()) {
            throw new IllegalArgumentException("Algunos créditos no existen o no pertenecen al usuario");
        }

        LocalDate today = LocalDate.now();
        for (Credit c : credits) {
            if (c.getStatus() != CreditStatusEnum.CREATED) {
                throw new IllegalArgumentException(
                        "Crédito " + c.getId() + " no está disponible para reservar. Estado: " + c.getStatus());
            }
            if (c.getExpirationDate().isBefore(today)) {
                throw new IllegalArgumentException("Crédito " + c.getId() + " está vencido");
            }
        }

        // Ordenar por monto descendente (mayor primero)
        credits.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        BigDecimal totalAvailable = credits.stream()
                .map(Credit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAvailable.compareTo(request.getAmountToReserve()) < 0) {
            throw new IllegalArgumentException(
                    String.format("Saldo insuficiente. Disponible: %.2f, a reservar: %.2f",
                            totalAvailable, request.getAmountToReserve()));
        }

        BigDecimal remainingToReserve = request.getAmountToReserve();
        List<Long> creditIdsReserved = new ArrayList<>();
        List<Credit> toSave = new ArrayList<>();

        for (Credit credit : credits) {
            if (remainingToReserve.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal creditAmount = credit.getAmount();
            BigDecimal reserveThis = remainingToReserve.min(creditAmount);
            if (reserveThis.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            credit.setReservedAmount(reserveThis);
            credit.setStatus(CreditStatusEnum.RESERVED);
            credit.setShoppingCartItemId(request.getShoppingCartItemId());
            remainingToReserve = remainingToReserve.subtract(reserveThis);
            creditIdsReserved.add(credit.getId());
            toSave.add(credit);
        }

        if (remainingToReserve.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "No se pudo cubrir el monto a reservar. Falta: " + remainingToReserve);
        }

        creditRepository.saveAll(toSave);
        log.info("Reserved {} credits for item {} total amount {}: creditIds={}",
                creditIdsReserved.size(), request.getShoppingCartItemId(), request.getAmountToReserve(), creditIdsReserved);

        return ReserveCreditResponse.builder()
                .shoppingCartItemId(request.getShoppingCartItemId())
                .amountReserved(request.getAmountToReserve())
                .creditIdsReserved(creditIdsReserved)
                .build();
    }
}

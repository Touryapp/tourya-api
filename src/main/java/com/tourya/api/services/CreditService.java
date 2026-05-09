package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.Credit;
import com.tourya.api.models.Role;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.CreditResponse;
import com.tourya.api.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de créditos.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreditService {

    private final CreditRepository creditRepository;

    /**
     * Obtiene todos los créditos según el rol del usuario.
     * - Back office: retorna todos los créditos (opcionalmente filtrados por status)
     * - Usuario normal: retorna solo los créditos de sus reservas (opcionalmente filtrados por status)
     * 
     * @param authentication Autenticación del usuario
     * @param status Estado del crédito para filtrar (opcional, si es null retorna todos)
     * @return Lista de CreditResponse
     */
    @Transactional(readOnly = true)
    public List<CreditResponse> getAllCredits(Authentication authentication, CreditStatusEnum status) {
        User user = (User) authentication.getPrincipal();
        List<Role> roles = user.getRoles();

        List<Credit> credits;

        if (Utils.isAdmin(roles)) {
            // Back office: todos los créditos
            if (status != null) {
                credits = creditRepository.findByStatus(status);
            } else {
                credits = creditRepository.findAll();
            }
            log.info("Back office user {} retrieving {} credits with status filter: {}", 
                    user.getId(), credits.size(), status);
        } else {
            // Usuario normal: solo créditos de sus reservas
            credits = creditRepository.findByUserId(user.getId(), status);
            log.info("User {} retrieving {} credits with status filter: {}", 
                    user.getId(), credits.size(), status);
        }

        return credits.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mapea un Credit a CreditResponse.
     */
    private CreditResponse mapToResponse(Credit credit) {
        return CreditResponse.builder()
                .id(credit.getId())
                .reservationId(credit.getReservationId())
                .amount(credit.getAmount())
                .reservedAmount(credit.getReservedAmount() != null ? credit.getReservedAmount() : java.math.BigDecimal.ZERO)
                .shoppingCartItemId(credit.getShoppingCartItemId())
                .creationDate(credit.getCreationDate())
                .expirationDate(credit.getExpirationDate())
                .status(credit.getStatus())
                .build();
    }
}

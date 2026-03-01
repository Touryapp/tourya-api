package com.tourya.api.services;

import com.tourya.api._utils.Utils;
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
     * - Back office: retorna todos los créditos
     * - Usuario normal: retorna solo los créditos de sus reservas
     * 
     * @param authentication Autenticación del usuario
     * @return Lista de CreditResponse
     */
    @Transactional(readOnly = true)
    public List<CreditResponse> getAllCredits(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Role> roles = user.getRoles();

        List<Credit> credits;

        if (Utils.isAdmin(roles)) {
            // Back office: todos los créditos
            credits = creditRepository.findAll();
            log.info("Back office user {} retrieving all credits", user.getId());
        } else {
            // Usuario normal: solo créditos de sus reservas
            // Usa consulta optimizada que hace JOIN directamente
            credits = creditRepository.findByUserId(user.getId());
            log.info("User {} retrieving {} credits", user.getId(), credits.size());
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
                .creationDate(credit.getCreationDate())
                .expirationDate(credit.getExpirationDate())
                .status(credit.getStatus())
                .build();
    }
}

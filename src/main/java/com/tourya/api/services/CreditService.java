package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.exceptions.OperationNotPermittedException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.Credit;
import com.tourya.api.models.Role;
import com.tourya.api.models.TouristProfile;
import com.tourya.api.models.User;
import com.tourya.api.models.request.TransferCreditRequest;
import com.tourya.api.models.responses.CreditResponse;
import com.tourya.api.models.responses.TouristLookupResponse;
import com.tourya.api.repository.CreditRepository;
import com.tourya.api.repository.TouristProfileRepository;
import com.tourya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreditService {

    private final CreditRepository creditRepository;
    private final TouristProfileRepository touristProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CreditResponse> getAllCredits(Authentication authentication, CreditStatusEnum status) {
        User user = (User) authentication.getPrincipal();
        List<Role> roles = user.getRoles();

        List<Credit> credits;
        if (Utils.isAdmin(roles)) {
            credits = status != null ? creditRepository.findByStatus(status) : creditRepository.findAll();
        } else {
            credits = creditRepository.findByUserId(user.getId(), status);
        }

        return credits.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TouristLookupResponse lookupTouristByDocument(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new OperationNotPermittedException("documentNumber is required");
        }
        TouristProfile profile = touristProfileRepository
                .findByDocumentNumberIgnoreCase(documentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Tourist not found for document: " + documentNumber));

        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return TouristLookupResponse.builder()
                .userId(user.getId())
                .fullName(user.fullName())
                .email(user.getEmail())
                .documentNumber(profile.getDocumentNumber())
                .build();
    }

    public CreditResponse transferCredit(Long creditId, TransferCreditRequest request, Authentication authentication) {
        User owner = (User) authentication.getPrincipal();
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit not found: " + creditId));

        if (!credit.getUserId().equals(owner.getId())) {
            throw new OperationNotPermittedException("You can only transfer your own credits");
        }
        if (credit.getTransferredAt() != null) {
            throw new OperationNotPermittedException("This credit has already been transferred");
        }
        if (credit.getStatus() != CreditStatusEnum.CREATED) {
            throw new OperationNotPermittedException("Only active credits can be transferred");
        }
        if (credit.getReservedAmount() != null
                && credit.getReservedAmount().compareTo(BigDecimal.ZERO) > 0
                && credit.getShoppingCartItemId() != null) {
            throw new OperationNotPermittedException("Cannot transfer a credit reserved for checkout");
        }
        if (credit.getExpirationDate() != null && credit.getExpirationDate().isBefore(LocalDate.now())) {
            throw new OperationNotPermittedException("Cannot transfer an expired credit");
        }
        if (request.getTargetUserId().equals(owner.getId())) {
            throw new OperationNotPermittedException("Cannot transfer credit to yourself");
        }

        userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        credit.setTransferredFromUserId(owner.getId());
        credit.setTransferredAt(LocalDateTime.now());
        credit.setUserId(request.getTargetUserId());
        credit = creditRepository.save(credit);

        log.info("Credit {} transferred from user {} to user {}", creditId, owner.getId(), request.getTargetUserId());
        return mapToResponse(credit);
    }

    private CreditResponse mapToResponse(Credit credit) {
        return CreditResponse.builder()
                .id(credit.getId())
                .reservationId(credit.getReservationId())
                .userId(credit.getUserId())
                .transferredFromUserId(credit.getTransferredFromUserId())
                .transferredAt(credit.getTransferredAt())
                .amount(credit.getAmount())
                .reservedAmount(credit.getReservedAmount() != null ? credit.getReservedAmount() : BigDecimal.ZERO)
                .shoppingCartItemId(credit.getShoppingCartItemId())
                .creationDate(credit.getCreationDate())
                .expirationDate(credit.getExpirationDate())
                .status(credit.getStatus())
                .build();
    }
}

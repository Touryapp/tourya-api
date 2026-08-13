package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreditService {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final long REFUND_PROOF_MAX_BYTES = 5L * 1024 * 1024; // 5MB
    private static final String REFUND_PROOF_PREFIX = "credit-refund-proofs";
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    private final CreditRepository creditRepository;
    private final TouristProfileRepository touristProfileRepository;
    private final UserRepository userRepository;
    private final IStorageService storageService;

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

    /**
     * TC-022 (#253): el turista solicita la devolucion en efectivo del credito.
     * Transicion permitida: CREATED -> REFUND_REQUESTED, solo si el saldo libre
     * (amount - reservedAmount) es mayor a 0 y el credito pertenece al usuario.
     */
    public CreditResponse requestRefund(Long creditId, Authentication authentication) {
        User owner = (User) authentication.getPrincipal();
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit not found: " + creditId));

        if (!credit.getUserId().equals(owner.getId())) {
            throw new OperationNotPermittedException("You can only request a refund on your own credits");
        }
        if (credit.getStatus() != CreditStatusEnum.CREATED) {
            throw new OperationNotPermittedException(
                    "Only credits in CREATED status can be refunded (current: " + credit.getStatus() + ")");
        }
        if (credit.getExpirationDate() != null && credit.getExpirationDate().isBefore(LocalDate.now(BOGOTA))) {
            throw new OperationNotPermittedException("Cannot request refund on an expired credit");
        }
        BigDecimal reserved = credit.getReservedAmount() != null ? credit.getReservedAmount() : BigDecimal.ZERO;
        BigDecimal available = credit.getAmount().subtract(reserved);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperationNotPermittedException(
                    "Credit has no free balance available for refund (reserved for cart)");
        }

        credit.setStatus(CreditStatusEnum.REFUND_REQUESTED);
        credit.setRefundRequestedAt(LocalDateTime.now(BOGOTA));
        credit = creditRepository.save(credit);

        log.info("Credit {} refund requested by user {}", creditId, owner.getId());
        return mapToResponse(credit);
    }

    /**
     * TC-022 (#253): ADMIN/BACKOFFICE_OPERATION sube el comprobante y marca la
     * devolucion como completada. Transicion: REFUND_REQUESTED -> REFUNDED.
     */
    public CreditResponse uploadRefundProof(Long creditId, MultipartFile proof, Authentication authentication)
            throws IOException {
        User user = (User) authentication.getPrincipal();
        if (!Utils.isTouryaBackoffice(user.getRoles())) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

        validateRefundProofFile(proof);

        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit not found: " + creditId));

        if (credit.getStatus() != CreditStatusEnum.REFUND_REQUESTED) {
            throw new OperationNotPermittedException(
                    "Only credits in REFUND_REQUESTED status can be marked as REFUNDED (current: "
                            + credit.getStatus() + ")");
        }

        String url = storageService.uploadFile(REFUND_PROOF_PREFIX + "/" + creditId, proof);

        credit.setStatus(CreditStatusEnum.REFUNDED);
        credit.setRefundedAt(LocalDateTime.now(BOGOTA));
        credit.setRefundProofUrl(url);
        credit = creditRepository.save(credit);

        log.info("Credit {} refunded by admin/backoffice user {} with proof {}", creditId, user.getId(), url);
        return mapToResponse(credit);
    }

    /**
     * TC-022 (#253): listado global paginado para ADMIN/BACKOFFICE_OPERATION.
     * Retorna todos los creditos (filtrando opcionalmente por status) con datos
     * embebidos del turista (touristName, touristEmail).
     */
    @Transactional(readOnly = true)
    public Page<CreditResponse> findAllForAdmin(Authentication authentication,
                                                CreditStatusEnum status,
                                                Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        if (!Utils.isTouryaBackoffice(user.getRoles())) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

        Page<Credit> page = (status != null)
                ? creditRepository.findByStatus(status, pageable)
                : creditRepository.findAll(pageable);

        Map<Integer, User> usersById = loadUsersForCredits(page.getContent());
        return page.map(c -> mapToResponseWithTourist(c, usersById.get(c.getUserId())));
    }

    private Map<Integer, User> loadUsersForCredits(List<Credit> credits) {
        if (credits.isEmpty()) {
            return Map.of();
        }
        Set<Integer> userIds = credits.stream()
                .map(Credit::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Integer, User> byId = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> byId.put(u.getId(), u));
        return byId;
    }

    private void validateRefundProofFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Refund proof file is required");
        }
        if (file.getSize() > REFUND_PROOF_MAX_BYTES) {
            throw new IllegalArgumentException("Refund proof too large. Max 5MB");
        }
        String ct = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        boolean ok = ct.equals("application/pdf")
                || ct.equals("image/png")
                || ct.equals("image/jpeg")
                || ct.equals("image/jpg");
        if (!ok) {
            throw new IllegalArgumentException("Invalid file type. Only PDF or JPG/PNG images are allowed");
        }
    }

    private CreditResponse mapToResponse(Credit credit) {
        return mapToResponseWithTourist(credit, null);
    }

    private CreditResponse mapToResponseWithTourist(Credit credit, User tourist) {
        CreditResponse.CreditResponseBuilder builder = CreditResponse.builder()
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
                .refundRequestedAt(credit.getRefundRequestedAt())
                .refundedAt(credit.getRefundedAt())
                .refundProofUrl(credit.getRefundProofUrl());
        if (tourist != null) {
            builder.touristName(tourist.fullName()).touristEmail(tourist.getEmail());
        }
        return builder.build();
    }
}

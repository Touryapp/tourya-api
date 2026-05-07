package com.tourya.api.services;

import com.tourya.api._utils.Utils;
import com.tourya.api.constans.enums.AccountPayableStatusEnum;
import com.tourya.api.constans.enums.ProviderPayoutOrderStatusEnum;
import com.tourya.api.exceptions.InsufficientPrivilegesException;
import com.tourya.api.exceptions.ResourceNotFoundException;
import com.tourya.api.models.AccountPayable;
import com.tourya.api.models.Provider;
import com.tourya.api.models.ProviderPayoutAttachment;
import com.tourya.api.models.ProviderPayoutOrder;
import com.tourya.api.models.ProviderPayoutOrderReservation;
import com.tourya.api.models.Reservation;
import com.tourya.api.models.Role;
import com.tourya.api.models.User;
import com.tourya.api.models.responses.ProviderPayoutOrderDetailsResponse;
import com.tourya.api.models.responses.ProviderPayoutOrderListItemResponse;
import com.tourya.api.repository.AccountPayableRepository;
import com.tourya.api.repository.ProviderPayoutAttachmentRepository;
import com.tourya.api.repository.ProviderPayoutOrderRepository;
import com.tourya.api.repository.ProviderPayoutOrderReservationRepository;
import com.tourya.api.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderPayoutOrderService {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final String NOT_PRIVILEGES = "You have no privileges to perform this action.";

    private final ProviderService providerService;
    private final S3Service s3Service;

    private final ProviderPayoutOrderRepository payoutOrderRepository;
    private final ProviderPayoutOrderReservationRepository payoutOrderReservationRepository;
    private final ProviderPayoutAttachmentRepository payoutAttachmentRepository;

    private final AccountPayableRepository accountPayableRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<ProviderPayoutOrderListItemResponse> listForProvider(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Provider provider = providerService.findByUserAndStatusActive(user);

        return payoutOrderRepository.findAll().stream()
                .filter(o -> o.getProviderId().equals(provider.getId()))
                .map(o -> ProviderPayoutOrderListItemResponse.builder()
                        .id(o.getId())
                        .providerId(o.getProviderId())
                        .createdAt(o.getCreatedAt())
                        .payDate(o.getPayDate())
                        .status(o.getStatus())
                        .amountTotal(o.getAmountTotal())
                        .reservationsCount(payoutOrderReservationRepository.findByPayoutOrderId(o.getId()).size())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderPayoutOrderListItemResponse> listForAdmin(Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        List<Role> roles = user.getRoles();
        if (!Utils.isAdmin(roles)) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

        return payoutOrderRepository.findAll().stream()
                .map(o -> ProviderPayoutOrderListItemResponse.builder()
                        .id(o.getId())
                        .providerId(o.getProviderId())
                        .createdAt(o.getCreatedAt())
                        .payDate(o.getPayDate())
                        .status(o.getStatus())
                        .amountTotal(o.getAmountTotal())
                        .reservationsCount(payoutOrderReservationRepository.findByPayoutOrderId(o.getId()).size())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderPayoutOrderDetailsResponse getDetailsForProvider(Long orderId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Provider provider = providerService.findByUserAndStatusActive(user);

        ProviderPayoutOrder order = payoutOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout order not found: " + orderId));

        if (!order.getProviderId().equals(provider.getId())) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

        return buildDetails(order);
    }

    @Transactional(readOnly = true)
    public ProviderPayoutOrderDetailsResponse getDetailsForAdmin(Long orderId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        List<Role> roles = user.getRoles();
        if (!Utils.isAdmin(roles)) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

        ProviderPayoutOrder order = payoutOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout order not found: " + orderId));

        return buildDetails(order);
    }

    @Transactional
    public ProviderPayoutOrderDetailsResponse uploadAttachmentAndMarkPaid(Long orderId, MultipartFile file, Authentication connectedUser) throws IOException {
        User user = (User) connectedUser.getPrincipal();
        List<Role> roles = user.getRoles();
        if (!Utils.isAdmin(roles)) {
            throw new InsufficientPrivilegesException(NOT_PRIVILEGES);
        }

        validateProofFile(file);

        ProviderPayoutOrder order = payoutOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout order not found: " + orderId));

        if (order.getStatus() != ProviderPayoutOrderStatusEnum.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be marked as PAID");
        }

        String url = s3Service.uploadFile("provider-payout-orders/" + orderId, file);
        ProviderPayoutAttachment att = new ProviderPayoutAttachment();
        att.setPayoutOrderId(orderId);
        att.setFileUrl(url);
        att.setCreatedAt(OffsetDateTime.now(ZONE));
        payoutAttachmentRepository.save(att);

        // Mark order paid + propagate to account payable + reservation payout status
        order.setStatus(ProviderPayoutOrderStatusEnum.PAID);
        payoutOrderRepository.save(order);

        List<ProviderPayoutOrderReservation> links = payoutOrderReservationRepository.findByPayoutOrderId(orderId);

        for (ProviderPayoutOrderReservation link : links) {
            if (link.getAccountPayableId() != null) {
                AccountPayable ap = accountPayableRepository.findById(link.getAccountPayableId()).orElse(null);
                if (ap != null) {
                    ap.setDeliveryStatus(AccountPayableStatusEnum.PAID);
                    accountPayableRepository.save(ap);
                }
            }

            Reservation r = reservationRepository.findById(link.getReservationId()).orElse(null);
            if (r != null) {
                r.setPayoutStatus("PAID");
                r.setPayoutPaidAt(OffsetDateTime.now(ZONE));
                reservationRepository.save(r);
            }
        }

        return buildDetails(order);
    }

    private ProviderPayoutOrderDetailsResponse buildDetails(ProviderPayoutOrder order) {
        List<ProviderPayoutAttachment> attachments = payoutAttachmentRepository.findByPayoutOrderId(order.getId());

        List<ProviderPayoutOrderReservation> links = payoutOrderReservationRepository.findByPayoutOrderId(order.getId());

        List<ProviderPayoutOrderDetailsResponse.Attachment> attDtos = attachments.stream()
                .map(a -> ProviderPayoutOrderDetailsResponse.Attachment.builder()
                        .id(a.getId())
                        .fileUrl(a.getFileUrl())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        List<ProviderPayoutOrderDetailsResponse.Item> items = links.stream()
                .map(l -> {
                    Reservation r = reservationRepository.findById(l.getReservationId()).orElse(null);
                    return ProviderPayoutOrderDetailsResponse.Item.builder()
                            .reservationId(l.getReservationId())
                            .accountPayableId(l.getAccountPayableId())
                            .amount(l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                            .payoutAvailableDate(r != null ? r.getPayoutAvailableDate() : null)
                            .payoutStatus(r != null ? r.getPayoutStatus() : null)
                            .build();
                })
                .toList();

        return ProviderPayoutOrderDetailsResponse.builder()
                .id(order.getId())
                .providerId(order.getProviderId())
                .createdAt(order.getCreatedAt())
                .payDate(order.getPayDate())
                .status(order.getStatus())
                .amountTotal(order.getAmountTotal())
                .attachments(attDtos)
                .reservations(items)
                .build();
    }

    private void validateProofFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > 1_048_576) { // 1MB
            throw new IllegalArgumentException("File too large. Max 1MB");
        }

        String ct = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        boolean ok = ct.equals("application/pdf")
                || ct.equals("image/png")
                || ct.equals("image/jpeg")
                || ct.equals("image/jpg")
                || ct.equals("image/webp");
        if (!ok) {
            throw new IllegalArgumentException("Invalid file type. Only PDF or images are allowed");
        }
    }
}


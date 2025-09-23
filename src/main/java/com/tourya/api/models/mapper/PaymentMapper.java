package com.tourya.api.models.mapper;

import com.tourya.api.models.Payment;
import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.responses.PaymentResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre entidades Payment y DTOs.
 * 
 * @author Tourya API Team
 * @version 1.0
 */
@Component
public class PaymentMapper {

    /**
     * Convierte un CreatePaymentRequest a entidad Payment
     */
    public Payment toEntity(CreatePaymentRequest request) {
        if (request == null) {
            return null;
        }

        return Payment.builder()
                .transactionId(request.getTransactionId())
                .status(request.getStatus())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethodType(request.getPaymentMethodType())
                .productId(request.getProductId())
                .productType(request.getProductType())
                .scheduleDate(request.getScheduleDate())
                .tourScheduleId(request.getTourScheduleId())
                .slotId(request.getSlotId())
                .itemTotalPrice(request.getItemTotalPrice())
                .itemStatus(request.getItemStatus())
                .payerName(request.getPayerName())
                .payerEmail(request.getPayerEmail())
                .payerId(request.getPayerId())
                .payerPhone(request.getPayerPhone())
                .payerDocumentType(request.getPayerDocumentType())
                .payerDocumentNumber(request.getPayerDocumentNumber())
                .build();
    }

    /**
     * Convierte una entidad Payment a PaymentResponse
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethodType(payment.getPaymentMethodType())
                .shoppingCartItemId(payment.getShoppingCartItem() != null ? payment.getShoppingCartItem().getId() : null)
                .productId(payment.getProductId())
                .productType(payment.getProductType())
                .scheduleDate(payment.getScheduleDate())
                .tourScheduleId(payment.getTourScheduleId())
                .slotId(payment.getSlotId())
                .itemTotalPrice(payment.getItemTotalPrice())
                .itemStatus(payment.getItemStatus())
                .payerName(payment.getPayerName())
                .payerEmail(payment.getPayerEmail())
                .payerId(payment.getPayerId())
                .payerPhone(payment.getPayerPhone())
                .payerDocumentType(payment.getPayerDocumentType())
                .payerDocumentNumber(payment.getPayerDocumentNumber())
                .createdDate(payment.getCreatedDate())
                .lastModifiedDate(payment.getLastModifiedDate())
                .createdBy(payment.getCreatedBy())
                .lastModifiedBy(payment.getLastModifiedBy())
                .build();
    }

    /**
     * Actualiza una entidad Payment con datos de un request
     */
    public void updateEntity(Payment payment, CreatePaymentRequest request) {
        if (payment == null || request == null) {
            return;
        }

        payment.setTransactionId(request.getTransactionId());
        payment.setStatus(request.getStatus());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethodType(request.getPaymentMethodType());
        payment.setProductId(request.getProductId());
        payment.setProductType(request.getProductType());
        payment.setScheduleDate(request.getScheduleDate());
        payment.setTourScheduleId(request.getTourScheduleId());
        payment.setSlotId(request.getSlotId());
        payment.setItemTotalPrice(request.getItemTotalPrice());
        payment.setItemStatus(request.getItemStatus());
        payment.setPayerName(request.getPayerName());
        payment.setPayerEmail(request.getPayerEmail());
        payment.setPayerId(request.getPayerId());
        payment.setPayerPhone(request.getPayerPhone());
        payment.setPayerDocumentType(request.getPayerDocumentType());
        payment.setPayerDocumentNumber(request.getPayerDocumentNumber());
    }
}

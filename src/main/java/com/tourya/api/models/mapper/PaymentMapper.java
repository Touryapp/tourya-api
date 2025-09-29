package com.tourya.api.models.mapper;

import com.tourya.api.models.Payment;
import com.tourya.api.models.request.CreatePaymentRequest;
import com.tourya.api.models.responses.PaymentResponse;
import com.tourya.api.models.responses.PayerResponse;
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
                .transactionData(request.getTransactionData())
                .payerId(request.getPayer().getId())
                .payerName(request.getPayer().getName())
                .payerEmail(request.getPayer().getEmail())
                .payerPhone(request.getPayer().getPhone())
                .payerDocumentType(request.getPayer().getDocumentType())
                .payerDocumentNumber(request.getPayer().getDocumentNumber())
                .build();
    }

    /**
     * Convierte una entidad Payment a PaymentResponse
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        // Crear PayerResponse
        PayerResponse payerResponse = PayerResponse.builder()
                .id(payment.getPayerId())
                .name(payment.getPayerName())
                .email(payment.getPayerEmail())
                .phone(payment.getPayerPhone())
                .documentType(payment.getPayerDocumentType())
                .documentNumber(payment.getPayerDocumentNumber())
                .build();

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionId(payment.getTransactionId())
                .transactionData(payment.getTransactionData())
                .reservation(null) // Se llenará desde el servicio si es necesario
                .payer(payerResponse)
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
        payment.setTransactionData(request.getTransactionData());
        payment.setPayerId(request.getPayer().getId());
        payment.setPayerName(request.getPayer().getName());
        payment.setPayerEmail(request.getPayer().getEmail());
        payment.setPayerPhone(request.getPayer().getPhone());
        payment.setPayerDocumentType(request.getPayer().getDocumentType());
        payment.setPayerDocumentNumber(request.getPayer().getDocumentNumber());
    }
}
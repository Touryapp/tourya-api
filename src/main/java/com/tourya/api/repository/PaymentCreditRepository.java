package com.tourya.api.repository;

import com.tourya.api.models.PaymentCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCreditRepository extends JpaRepository<PaymentCredit, Long> {

    List<PaymentCredit> findByPaymentId(Long paymentId);
}

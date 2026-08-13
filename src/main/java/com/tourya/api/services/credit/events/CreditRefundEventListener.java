package com.tourya.api.services.credit.events;

import com.tourya.api.models.User;
import com.tourya.api.repository.UserRepository;
import com.tourya.api.services.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

/**
 * TC-022 (#253): reacciona a {@link CreditRefundEvent} DESPUES del commit de la
 * tx que actualiza el credito. Envia el email correspondiente al turista.
 *
 * <p>Sin {@code @Async} en el listener — los metodos de {@link EmailService}
 * ya son {@code @Async}, asi que la carga de SMTP se despacha al executor sin
 * bloquear la respuesta HTTP. Se evita el bug observado en
 * {@code MaritimeAlertEventListener} (async + AFTER_COMMIT no disparaba en
 * dev).</p>
 *
 * <p>Ninguna excepcion propaga fuera del listener: si el email falla, se
 * loguea WARN y termina. El estado del credito ya quedo consistente en BD.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditRefundEventListener {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CreditRefundEvent.RefundRequested event) {
        Optional<User> user = userRepository.findById(event.userId());
        if (user.isEmpty() || user.get().getEmail() == null || user.get().getEmail().isBlank()) {
            log.warn("TC-022 RefundRequested credit {} without user or email, skipping email",
                    event.creditId());
            return;
        }
        try {
            emailService.sendCreditRefundRequestedEmail(
                    user.get().getEmail(),
                    buildDisplayName(user.get()),
                    event.creditId(),
                    event.reservationId(),
                    event.amount(),
                    event.refundRequestedAt(),
                    "Tu solicitud de devolucion fue recibida - Credito #" + event.creditId()
            );
        } catch (MessagingException | RuntimeException e) {
            log.warn("TC-022 RefundRequested email failed for credit {}: {}",
                    event.creditId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CreditRefundEvent.RefundCompleted event) {
        Optional<User> user = userRepository.findById(event.userId());
        if (user.isEmpty() || user.get().getEmail() == null || user.get().getEmail().isBlank()) {
            log.warn("TC-022 RefundCompleted credit {} without user or email, skipping email",
                    event.creditId());
            return;
        }
        try {
            emailService.sendCreditRefundCompletedEmail(
                    user.get().getEmail(),
                    buildDisplayName(user.get()),
                    event.creditId(),
                    event.reservationId(),
                    event.amount(),
                    event.refundedAt(),
                    event.proofUrl(),
                    "Tu devolucion fue procesada - Credito #" + event.creditId()
            );
        } catch (MessagingException | RuntimeException e) {
            log.warn("TC-022 RefundCompleted email failed for credit {}: {}",
                    event.creditId(), e.getMessage());
        }
    }

    private String buildDisplayName(User user) {
        String first = user.getFirstname();
        if (first == null || first.isBlank()) {
            return user.getEmail();
        }
        return first;
    }
}

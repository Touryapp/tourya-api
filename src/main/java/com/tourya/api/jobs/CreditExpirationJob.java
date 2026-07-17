package com.tourya.api.jobs;

import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.Credit;
import com.tourya.api.models.User;
import com.tourya.api.repository.CreditRepository;
import com.tourya.api.repository.UserRepository;
import com.tourya.api.services.EmailService;
import com.tourya.api.services.push.PushDomainEvent;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * BE-18 (recordatorios 30 y 7 dias antes) + BE-19 (marca EXPIRED y notifica).
 *
 * <p>Corre a las 5:00 AM Bogota, igual patron que {@link ReservationCancellationFlagsJob}.
 * Cada corrida hace 3 pases:</p>
 * <ol>
 *   <li>Reminder 30 dias: creditos {@code CREATED} con {@code expiration_date =
 *   today + 30} y {@code reminder_30d_sent_at IS NULL}.</li>
 *   <li>Reminder 7 dias: idem con 7 dias y {@code reminder_7d_sent_at}.</li>
 *   <li>Expirados: creditos {@code CREATED} con {@code expiration_date < today}
 *   pasan a {@code EXPIRED} y se notifica una vez.</li>
 * </ol>
 *
 * <p>Idempotencia via los timestamps: si el job corre 2 veces el mismo dia,
 * los creditos ya notificados no se reprocesan (el query los filtra por
 * {@code IS NULL}).</p>
 *
 * <p>Si el envio de correo falla para un credito, se loguea WARN y el
 * timestamp NO se marca — la proxima corrida reintenta ese credito. Esto
 * evita perder notificaciones por errores transitorios de SMTP.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditExpirationJob {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final int REMINDER_30D_DAYS = 30;
    private static final int REMINDER_7D_DAYS = 7;

    private final CreditRepository creditRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher; // MO-40b

    @Value("${application.mailing.frontend.credit-url:https://tourya.co/}")
    private String creditUsageUrl;

    @Scheduled(cron = "0 0 5 * * *", zone = "America/Bogota")
    @Transactional
    public void runDaily() {
        LocalDate today = LocalDate.now(BOGOTA);
        int r30 = sendReminders(today.plusDays(REMINDER_30D_DAYS), REMINDER_30D_DAYS, true);
        int r7 = sendReminders(today.plusDays(REMINDER_7D_DAYS), REMINDER_7D_DAYS, false);
        int exp = markAndNotifyExpired(today);
        log.info("CreditExpirationJob: reminders30d={}, reminders7d={}, expired={}", r30, r7, exp);
    }

    private int sendReminders(LocalDate targetDate, int daysAdvance, boolean is30d) {
        List<Credit> candidates = is30d
                ? creditRepository.findExpiringInNeedingReminder30d(targetDate)
                : creditRepository.findExpiringInNeedingReminder7d(targetDate);
        int sent = 0;
        for (Credit credit : candidates) {
            Optional<User> user = userRepository.findById(credit.getUserId());
            if (user.isEmpty() || user.get().getEmail() == null || user.get().getEmail().isBlank()) {
                log.warn("CreditExpirationJob: credit {} without user or email, skipping reminder{}d",
                        credit.getId(), daysAdvance);
                continue;
            }
            try {
                emailService.sendCreditExpiringReminder(
                        user.get().getEmail(),
                        buildDisplayName(user.get()),
                        credit.getAmount(),
                        credit.getExpirationDate(),
                        daysAdvance,
                        creditUsageUrl,
                        "Tu crédito Tourya vence en " + daysAdvance + " días"
                );
                if (is30d) {
                    credit.setReminder30dSentAt(LocalDateTime.now());
                } else {
                    credit.setReminder7dSentAt(LocalDateTime.now());
                }
                creditRepository.save(credit);
                // MO-40b: push via evento AFTER_COMMIT (paralelo al email)
                eventPublisher.publishEvent(new PushDomainEvent.CreditExpiringSoon(
                        credit.getUserId(), daysAdvance));
                sent++;
            } catch (MessagingException | RuntimeException e) {
                log.warn("CreditExpirationJob: reminder{}d failed for credit {}: {}",
                        daysAdvance, credit.getId(), e.getMessage());
            }
        }
        return sent;
    }

    private int markAndNotifyExpired(LocalDate today) {
        List<Credit> expired = creditRepository.findExpiredNotYetMarked(today);
        int notified = 0;
        for (Credit credit : expired) {
            credit.setStatus(CreditStatusEnum.EXPIRED);
            Optional<User> user = userRepository.findById(credit.getUserId());
            if (user.isEmpty() || user.get().getEmail() == null || user.get().getEmail().isBlank()) {
                log.warn("CreditExpirationJob: credit {} without user or email, marking EXPIRED without notification",
                        credit.getId());
                creditRepository.save(credit);
                continue;
            }
            try {
                emailService.sendCreditExpiredNotice(
                        user.get().getEmail(),
                        buildDisplayName(user.get()),
                        credit.getAmount(),
                        credit.getExpirationDate(),
                        "Tu crédito Tourya ha vencido"
                );
                credit.setExpiredNotifiedAt(LocalDateTime.now());
                // MO-40b: push via evento AFTER_COMMIT (paralelo al email)
                eventPublisher.publishEvent(new PushDomainEvent.CreditExpired(credit.getUserId()));
                notified++;
            } catch (MessagingException | RuntimeException e) {
                log.warn("CreditExpirationJob: expired notice failed for credit {}: {}",
                        credit.getId(), e.getMessage());
                // Aun asi marca EXPIRED — el estado en BD debe reflejar la realidad.
                // La proxima corrida no lo reprocesa porque ya no esta CREATED.
            }
            creditRepository.save(credit);
        }
        return notified;
    }

    private String buildDisplayName(User user) {
        String first = user.getFirstname();
        if (first == null || first.isBlank()) {
            return user.getEmail();
        }
        return first;
    }
}

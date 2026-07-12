package com.tourya.api.jobs;

import com.tourya.api.constans.enums.CreditStatusEnum;
import com.tourya.api.models.Credit;
import com.tourya.api.models.User;
import com.tourya.api.repository.CreditRepository;
import com.tourya.api.repository.UserRepository;
import com.tourya.api.services.EmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BE-18 (recordatorios) + BE-19 (marcar EXPIRED y notificar). Tests con Mockito
 * — sin BD ni SMTP reales. Cubren los 3 pases del job:
 * <ul>
 *   <li>Reminder 30d marca {@code reminder_30d_sent_at}.</li>
 *   <li>Reminder 7d marca {@code reminder_7d_sent_at}.</li>
 *   <li>Expirado pasa a status EXPIRED y setea {@code expired_notified_at}.</li>
 * </ul>
 * Verifican idempotencia (si el mail falla, el timestamp no se marca; en el caso
 * de EXPIRED el status se persiste igual porque debe reflejar la realidad de BD).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BE-18/19 CreditExpirationJob")
class CreditExpirationJobTest {

    private static final String USER_EMAIL = "turista@example.com";
    private static final String USER_FIRSTNAME = "Ana";
    private static final Integer USER_ID = 100;
    private static final Long CREDIT_ID = 500L;

    @Mock private CreditRepository creditRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private CreditExpirationJob job;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "creditUsageUrl", "https://tourya.co/mis-creditos");
        user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setFirstname(USER_FIRSTNAME);
    }

    private Credit newCredit() {
        Credit c = new Credit();
        c.setId(CREDIT_ID);
        c.setUserId(USER_ID);
        c.setAmount(new BigDecimal("50000"));
        c.setStatus(CreditStatusEnum.CREATED);
        c.setExpirationDate(LocalDate.now().plusDays(30));
        c.setReservedAmount(BigDecimal.ZERO);
        return c;
    }

    @Test
    @DisplayName("reminder 30d: envia correo y marca timestamp")
    void reminder30d_sendsEmailAndMarksTimestamp() throws Exception {
        Credit credit = newCredit();
        LocalDate today = LocalDate.now();
        when(creditRepository.findExpiringInNeedingReminder30d(today.plusDays(30)))
                .thenReturn(List.of(credit));
        when(creditRepository.findExpiringInNeedingReminder7d(today.plusDays(7)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiredNotYetMarked(today)).thenReturn(Collections.emptyList());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        job.runDaily();

        verify(emailService).sendCreditExpiringReminder(
                eq(USER_EMAIL),
                eq(USER_FIRSTNAME),
                eq(credit.getAmount()),
                eq(credit.getExpirationDate()),
                eq(30L),
                anyString(),
                anyString());
        ArgumentCaptor<Credit> saved = ArgumentCaptor.forClass(Credit.class);
        verify(creditRepository).save(saved.capture());
        assertThat(saved.getValue().getReminder30dSentAt()).isNotNull();
        assertThat(saved.getValue().getReminder7dSentAt()).isNull();
    }

    @Test
    @DisplayName("reminder 7d: envia correo y marca timestamp")
    void reminder7d_sendsEmailAndMarksTimestamp() throws Exception {
        Credit credit = newCredit();
        credit.setExpirationDate(LocalDate.now().plusDays(7));
        LocalDate today = LocalDate.now();
        when(creditRepository.findExpiringInNeedingReminder30d(today.plusDays(30)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiringInNeedingReminder7d(today.plusDays(7)))
                .thenReturn(List.of(credit));
        when(creditRepository.findExpiredNotYetMarked(today)).thenReturn(Collections.emptyList());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        job.runDaily();

        verify(emailService).sendCreditExpiringReminder(
                eq(USER_EMAIL),
                eq(USER_FIRSTNAME),
                eq(credit.getAmount()),
                eq(credit.getExpirationDate()),
                eq(7L),
                anyString(),
                anyString());
        ArgumentCaptor<Credit> saved = ArgumentCaptor.forClass(Credit.class);
        verify(creditRepository).save(saved.capture());
        assertThat(saved.getValue().getReminder7dSentAt()).isNotNull();
        assertThat(saved.getValue().getReminder30dSentAt()).isNull();
    }

    @Test
    @DisplayName("expirado: cambia status a EXPIRED, envia notice y marca timestamp")
    void expired_marksExpiredSendsNoticeAndTimestamps() throws Exception {
        Credit credit = newCredit();
        credit.setExpirationDate(LocalDate.now().minusDays(1));
        LocalDate today = LocalDate.now();
        when(creditRepository.findExpiringInNeedingReminder30d(today.plusDays(30)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiringInNeedingReminder7d(today.plusDays(7)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiredNotYetMarked(today)).thenReturn(List.of(credit));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        job.runDaily();

        verify(emailService).sendCreditExpiredNotice(
                eq(USER_EMAIL),
                eq(USER_FIRSTNAME),
                eq(credit.getAmount()),
                eq(credit.getExpirationDate()),
                anyString());
        ArgumentCaptor<Credit> saved = ArgumentCaptor.forClass(Credit.class);
        verify(creditRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(CreditStatusEnum.EXPIRED);
        assertThat(saved.getValue().getExpiredNotifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("expirado sin email: marca EXPIRED igual pero no notifica")
    void expired_withoutEmail_stillMarksExpired() {
        Credit credit = newCredit();
        credit.setExpirationDate(LocalDate.now().minusDays(1));
        User userNoEmail = new User();
        userNoEmail.setId(USER_ID);
        userNoEmail.setEmail(null);
        LocalDate today = LocalDate.now();
        when(creditRepository.findExpiringInNeedingReminder30d(today.plusDays(30)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiringInNeedingReminder7d(today.plusDays(7)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiredNotYetMarked(today)).thenReturn(List.of(credit));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userNoEmail));

        job.runDaily();

        ArgumentCaptor<Credit> saved = ArgumentCaptor.forClass(Credit.class);
        verify(creditRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(CreditStatusEnum.EXPIRED);
        assertThat(saved.getValue().getExpiredNotifiedAt()).isNull();
    }

    @Test
    @DisplayName("reminder falla en SMTP: no marca timestamp (reintenta al dia siguiente)")
    void reminder30d_smtpFails_doesNotMarkTimestamp() throws Exception {
        Credit credit = newCredit();
        LocalDate today = LocalDate.now();
        when(creditRepository.findExpiringInNeedingReminder30d(today.plusDays(30)))
                .thenReturn(List.of(credit));
        when(creditRepository.findExpiringInNeedingReminder7d(today.plusDays(7)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiredNotYetMarked(today)).thenReturn(Collections.emptyList());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        doThrow(new MessagingException("SMTP down")).when(emailService).sendCreditExpiringReminder(
                anyString(), anyString(), any(), any(), anyLong(), anyString(), anyString());

        job.runDaily();

        verify(creditRepository, never()).save(any());
    }

    @Test
    @DisplayName("reminder sin usuario: skipea sin exception")
    void reminder_userNotFound_skipsGracefully() throws Exception {
        Credit credit = newCredit();
        LocalDate today = LocalDate.now();
        when(creditRepository.findExpiringInNeedingReminder30d(today.plusDays(30)))
                .thenReturn(List.of(credit));
        when(creditRepository.findExpiringInNeedingReminder7d(today.plusDays(7)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiredNotYetMarked(today)).thenReturn(Collections.emptyList());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        job.runDaily();

        verify(emailService, never()).sendCreditExpiringReminder(
                anyString(), anyString(), any(), any(), anyLong(), anyString(), anyString());
        verify(creditRepository, never()).save(any());
    }

    @Test
    @DisplayName("dia sin candidatos: job corre y no envia nada")
    void emptyDay_runsSilently() {
        LocalDate today = LocalDate.now();
        when(creditRepository.findExpiringInNeedingReminder30d(today.plusDays(30)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiringInNeedingReminder7d(today.plusDays(7)))
                .thenReturn(Collections.emptyList());
        when(creditRepository.findExpiredNotYetMarked(today)).thenReturn(Collections.emptyList());

        job.runDaily();

        verify(creditRepository, never()).save(any());
    }
}

package com.tourya.api.services;

import com.tourya.api.constans.enums.EmailTemplateNameEnum;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmail(
            String to,
            String username,
            EmailTemplateNameEnum emailTemplate,
            String confirmationUrl,
            String activationCode,
            String subject
    ) throws MessagingException {
        String templateName;
        if (emailTemplate == null) {
            templateName = "confirm-email";
        } else {
            templateName = emailTemplate.getName();
        }
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name()
        );
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("confirmationUrl", confirmationUrl);
        properties.put("activation_code", activationCode);

        Context context = new Context();
        context.setVariables(properties);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String template = templateEngine.process(templateName, context);

        helper.setText(template, true);

        mailSender.send(mimeMessage);
    }

    @Async
    public void sendEmailTemporaryPassword(
            String to,
            String username,
            EmailTemplateNameEnum emailTemplate,
            String loginUrl,
            String passwordTemporary,
            String subject
    ) throws MessagingException {
        String templateName;
        if (emailTemplate == null) {
            templateName = "confirm-email";
        } else {
            templateName = emailTemplate.getName();
        }
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name()
        );
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("loginUrl", loginUrl);
        properties.put("password_temporary", passwordTemporary);

        Context context = new Context();
        context.setVariables(properties);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String template = templateEngine.process(templateName, context);

        helper.setText(template, true);

        mailSender.send(mimeMessage);
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail); // La dirección desde la que se enviará el correo
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    /**
     * BE-18 (RN-036): recordatorio de crédito por vencer. Se envia 30 y 7 dias
     * antes de {@code expirationDate} desde {@code CreditExpirationJob}.
     */
    @Async
    public void sendCreditExpiringReminder(
            String to,
            String username,
            java.math.BigDecimal amount,
            java.time.LocalDate expirationDate,
            long daysRemaining,
            String usageUrl,
            String subject
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name());
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("amount", amount);
        properties.put("expirationDate", expirationDate);
        properties.put("daysRemaining", daysRemaining);
        properties.put("usageUrl", usageUrl);
        Context context = new Context();
        context.setVariables(properties);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        String template = templateEngine.process("credit_expiring_reminder", context);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }

    /**
     * BE-19 (RN-036): notificacion de credito expirado.
     */
    @Async
    public void sendCreditExpiredNotice(
            String to,
            String username,
            java.math.BigDecimal amount,
            java.time.LocalDate expirationDate,
            String subject
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name());
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("amount", amount);
        properties.put("expirationDate", expirationDate);
        Context context = new Context();
        context.setVariables(properties);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        String template = templateEngine.process("credit_expired", context);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }

    /**
     * BE-24 (RN-055): notifica al turista que su reserva fue cancelada porque el provider
     * no puede atender, entrega el crédito compensatorio y lista de tours alternativos.
     */
    @Async
    public void sendProviderDeclinedNotification(
            String to,
            String username,
            String tourName,
            java.math.BigDecimal creditAmount,
            java.time.LocalDate creditExpirationDate,
            List<com.tourya.api.models.Tour> alternatives,
            String subject
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name());
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("tourName", tourName);
        properties.put("creditAmount", creditAmount);
        properties.put("creditExpirationDate", creditExpirationDate);
        // Presentar tours alternativos como estructuras simples (id, nombre, rating) para el template.
        List<Map<String, Object>> alternativesData = new java.util.ArrayList<>();
        if (alternatives != null) {
            for (com.tourya.api.models.Tour t : alternatives) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", t.getId());
                row.put("name", t.getName() != null ? t.getName().getEs() : "");
                row.put("rating", t.getRating() != null ? t.getRating() : 0);
                alternativesData.add(row);
            }
        }
        properties.put("alternatives", alternativesData);
        Context context = new Context();
        context.setVariables(properties);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        String template = templateEngine.process("provider_declined_notification", context);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }

    @Async
    public void sendPurchaseConfirmationEmail(
            String to,
            String username,
            List<com.tourya.api.models.responses.ReservationResponse> reservations,
            String subject
    ) throws MessagingException {
        String templateName = EmailTemplateNameEnum.PURCHASE_CONFIRMATION.getName();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name()
        );

        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("reservations", reservations);

        Context context = new Context();
        context.setVariables(properties);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String template = templateEngine.process(templateName, context);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }

    /**
     * TC-022 (#253): confirmacion al turista de que su solicitud de devolucion
     * de credito fue recibida (status CREATED -> REFUND_REQUESTED).
     */
    @Async
    public void sendCreditRefundRequestedEmail(
            String to,
            String username,
            Long creditId,
            Long reservationId,
            java.math.BigDecimal amount,
            java.time.LocalDateTime refundRequestedAt,
            String subject
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name());
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("creditId", creditId);
        properties.put("reservationId", reservationId);
        properties.put("amount", amount);
        properties.put("refundRequestedAt", formatBogotaDateTime(refundRequestedAt));
        Context context = new Context();
        context.setVariables(properties);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        String template = templateEngine.process("credit_refund_requested", context);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }

    /**
     * TC-022 (#253): notificacion al turista de que el reembolso fue procesado
     * (status REFUND_REQUESTED -> REFUNDED), con link al comprobante.
     */
    @Async
    public void sendCreditRefundCompletedEmail(
            String to,
            String username,
            Long creditId,
            Long reservationId,
            java.math.BigDecimal amount,
            java.time.LocalDateTime refundedAt,
            String proofUrl,
            String subject
    ) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name());
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("creditId", creditId);
        properties.put("reservationId", reservationId);
        properties.put("amount", amount);
        properties.put("refundedAt", formatBogotaDateTime(refundedAt));
        properties.put("proofUrl", proofUrl);
        Context context = new Context();
        context.setVariables(properties);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        String template = templateEngine.process("credit_refund_completed", context);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }

    /**
     * Renderiza un {@link java.time.LocalDateTime} en zona {@code America/Bogota}
     * con formato {@code dd/MM/yyyy HH:mm}. Los timestamps del dominio se
     * persisten en Bogota, se re-aplica la zona por seguridad.
     */
    private String formatBogotaDateTime(java.time.LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        java.time.ZoneId bogota = java.time.ZoneId.of("America/Bogota");
        return dt.atZone(bogota)
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @Async
    public void sendRequestProviderStatusEmail(
            String to,
            String username,
            String providerName,
            String statusLabel,
            String messageBody,
            String reason,
            String subject
    ) throws MessagingException {
        String templateName = EmailTemplateNameEnum.REQUEST_PROVIDER_STATUS.getName();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name()
        );

        Map<String, Object> properties = new HashMap<>();
        properties.put("username", username);
        properties.put("providerName", providerName);
        properties.put("statusLabel", statusLabel);
        properties.put("messageBody", messageBody);
        properties.put("reason", reason);

        Context context = new Context();
        context.setVariables(properties);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String template = templateEngine.process(templateName, context);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }
}

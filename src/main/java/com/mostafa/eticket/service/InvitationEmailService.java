package com.mostafa.eticket.service;

import com.mostafa.eticket.exception.InvitationEmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class InvitationEmailService {

    private static final String REGISTER_PATH = "/api/v1/auth/register/viewer";
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String baseUrl;
    private final String fromName;
    private final String from;

    public InvitationEmailService(JavaMailSender mailSender,
                                  SpringTemplateEngine templateEngine,
                                  @Value("${eticket.app.base-url}") String baseUrl,
                                  @Value("${eticket.mail.from-name}") String fromName,
                                  @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.baseUrl = baseUrl;
        this.fromName = fromName;
        this.from = from;
    }

    public void sendInvitation(String toEmail, String token, LocalDateTime expiresAt) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromName + " <" + from + ">");
            helper.setTo(toEmail);
            helper.setSubject("You're invited to E-Ticket");
            helper.setText(renderInvitation(token, expiresAt), true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new InvitationEmailException("Failed to send invitation to " + toEmail, e);
        }
    }

    private String renderInvitation(String token, LocalDateTime expiresAt) {
        String link = baseUrl + REGISTER_PATH + "?token=" + token;
        Context context = new Context();
        context.setVariables(Map.of(
                "link", link,
                "token", token,
                "expiresAt", expiresAt.format(EXPIRY_FORMATTER)));
        return templateEngine.process("email/invitation", context);
    }
}
package com.mostafa.eticket.service;

import com.mostafa.eticket.exception.InvitationEmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class InvitationEmailService {

    private static final String INVITATION_URL = "http://localhost:8080/api/v1/auth/register/viewer";

    private final JavaMailSender mailSender;
    private final String from;

    public InvitationEmailService(JavaMailSender mailSender,
                                  @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendInvitation(String toEmail, String token) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("You're invited to E-Ticket");
            helper.setText(buildHtmlBody(token), true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new InvitationEmailException("Failed to send invitation to " + toEmail, e);
        }
    }

    private String buildHtmlBody(String token) {
        return """
                <p>You've been invited to your organization's support desk on E-Ticket.</p>
                <p>Accept the invitation by registering as a viewer using the link below
                together with your invitation token.</p>
                <p><a href="%s">%s</a></p>
                <p><strong>Token: %s</strong></p>
                <p>This invitation expires in 24 hours and can only be used once.</p>
                """.formatted(INVITATION_URL, INVITATION_URL, token);
    }
}
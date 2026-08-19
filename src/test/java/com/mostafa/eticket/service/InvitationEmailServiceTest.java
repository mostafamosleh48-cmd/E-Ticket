package com.mostafa.eticket.service;

import com.mostafa.eticket.exception.InvitationEmailException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationEmailServiceTest {

    private JavaMailSender mailSender;
    private InvitationEmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new InvitationEmailService(mailSender, testTemplateEngine(),
                "http://localhost:8080", "E-Ticket Support", "mostafamosleh48@gmail.com");
    }

    private SpringTemplateEngine testTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void sendsInvitationToRecipientWithTokenAndConfigurableLink() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService.sendInvitation("viewer@b.com", "abc123", LocalDateTime.of(2026, 8, 20, 15, 0));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();

        assertThat(sent.getFrom()[0].toString()).isEqualTo("E-Ticket Support <mostafamosleh48@gmail.com>");
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("viewer@b.com");
        assertThat(sent.getSubject()).isEqualTo("You're invited to E-Ticket");
        assertThat((String) sent.getContent())
                .contains("http://localhost:8080/invitations/accept.html?token=abc123")
                .contains("abc123")
                .contains("2026-08-20 15:00");
    }

    @Test
    void propagatesMailFailureAsInvitationEmailException() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP rejected"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendInvitation(
                "viewer@b.com", "abc123", LocalDateTime.now()))
                .isInstanceOf(InvitationEmailException.class)
                .hasMessageContaining("viewer@b.com");
    }
}
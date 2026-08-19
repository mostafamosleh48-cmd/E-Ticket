package com.mostafa.eticket.service;

import com.mostafa.eticket.exception.InvitationEmailException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

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
        emailService = new InvitationEmailService(mailSender, "mostafamosleh48@gmail.com");
    }

    @Test
    void sendsInvitationToRecipientWithTokenInBody() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailService.sendInvitation("viewer@b.com", "abc123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();

        assertThat(sent.getFrom()[0].toString()).isEqualTo("mostafamosleh48@gmail.com");
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("viewer@b.com");
        assertThat(sent.getSubject()).isEqualTo("You're invited to E-Ticket");
        assertThat((String) sent.getContent())
                .contains("abc123")
                .contains("/api/v1/auth/register/viewer");
    }

    @Test
    void propagatesMailFailureAsInvitationEmailException() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP rejected"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendInvitation("viewer@b.com", "abc123"))
                .isInstanceOf(InvitationEmailException.class)
                .hasMessageContaining("viewer@b.com");
    }
}
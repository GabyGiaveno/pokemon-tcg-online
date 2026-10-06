package ar.edu.utn.frc.tup.piii.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void sendRecoveryEmail_whenMailNotConfigured_doesNotSendMail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "");

        emailService.sendRecoveryEmail("test@example.com", "ash", "http://localhost/reset");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void sendRecoveryEmail_whenMailConfigured_sendsMimeMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender, "smtp.example.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRecoveryEmail("test@example.com", "ash", "http://localhost/reset");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }
}

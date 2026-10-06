package ar.edu.utn.frc.tup.piii.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailConfigured;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.host:}") String mailHost) {
        this.mailSender = mailSender;
        this.mailConfigured = !mailHost.isBlank();
    }

    public void sendRecoveryEmail(String to, String username, String recoveryLink) {
        String subject = "Pokémon TCG — Restablecimiento de contraseña";
        String body = buildHtmlBody(username, recoveryLink);

        if (!mailConfigured) {
            log.info("=== EMAIL (mail not configured) ===");
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("Body: {}", body);
            log.info("=== END EMAIL ===");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Recovery email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send recovery email to {}: {}", to, e.getMessage());
        }
    }

    private String buildHtmlBody(String username, String recoveryLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f4f4f4;">
                <div style="max-width: 480px; margin: 0 auto; background: #fff; border-radius: 8px; padding: 32px;">
                    <h2 style="color: #e53935;">Pokémon TCG</h2>
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                    <p style="text-align: center; margin: 32px 0;">
                        <a href="%s"
                           style="background: #e53935; color: #fff; text-decoration: none;
                                  padding: 12px 28px; border-radius: 6px; display: inline-block;">
                            Restablecer contraseña
                        </a>
                    </p>
                    <p style="color: #666; font-size: 13px;">
                        Este enlace expira en 30 minutos. Si no solicitaste este cambio, ignorá este mensaje.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(username, recoveryLink);
    }
}

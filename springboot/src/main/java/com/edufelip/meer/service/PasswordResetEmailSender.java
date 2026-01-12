package com.edufelip.meer.service;

import com.edufelip.meer.domain.auth.PasswordResetNotifier;
import com.edufelip.meer.security.PasswordResetProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailSender implements PasswordResetNotifier {
  private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailSender.class);

  private final JavaMailSender mailSender;
  private final PasswordResetProperties properties;
  private final String fallbackFrom;

  public PasswordResetEmailSender(
      JavaMailSender mailSender,
      PasswordResetProperties properties,
      @Value("${spring.mail.username:}") String fallbackFrom) {
    this.mailSender = mailSender;
    this.properties = properties;
    this.fallbackFrom = fallbackFrom;
  }

  @Override
  public void sendResetLink(String email, String resetLink, long ttlMinutes) {
    if (email == null || email.isBlank()) {
      return;
    }

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setTo(email);

      String from = properties.getFrom();
      if (from == null || from.isBlank()) {
        from = fallbackFrom;
      }
      if (from != null && !from.isBlank()) {
        helper.setFrom(from);
      }

      String subject = properties.getSubject();
      helper.setSubject(subject == null || subject.isBlank() ? "Recuperação de Senha" : subject);

      String htmlContent = buildHtmlBody(resetLink, ttlMinutes);
      String textContent = buildTextBody(resetLink, ttlMinutes);

      helper.setText(textContent, htmlContent);

      mailSender.send(message);
    } catch (Exception ex) {
      log.warn("Failed to send password reset email to {}", email, ex);
    }
  }

  private String buildTextBody(String resetLink, long ttlMinutes) {
    return "Use este link para redefinir sua senha:\n\n"
        + resetLink
        + "\n\nEste link expira em "
        + ttlMinutes
        + " minutos.\n\n"
        + "Se você não solicitou isso, por favor ignore este e-mail.";
  }

  private String buildHtmlBody(String resetLink, long ttlMinutes) {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 0;">
          <div style="max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
            
            <!-- Header with Minimalist Badge -->
            <div style="background-color: #ffffff; padding: 40px 0 20px 0; text-align: center; border-bottom: 1px solid #f0f0f0;">
              <div style="width: 80px; height: 80px; background-color: #e6f0ff; border-radius: 50%%; margin: 0 auto; position: relative;">
                <!-- Sentinel Mascot Silhouette -->
                <div style="position: absolute; width: 24px; height: 45px; background-color: #007bff; border-radius: 12px 12px 4px 4px; bottom: 12px; left: 28px;"></div>
                <div style="position: absolute; width: 28px; height: 22px; background-color: #007bff; border-radius: 12px; top: 18px; left: 26px; border-bottom: 2px solid #0056b3;"></div>
                <div style="position: absolute; width: 8px; height: 8px; background-color: #007bff; border-radius: 50%%; top: 15px; left: 28px;"></div>
                <div style="position: absolute; width: 8px; height: 8px; background-color: #007bff; border-radius: 50%%; top: 15px; right: 28px;"></div>
              </div>
              <div style="margin-top: 15px; color: #1a202c; font-size: 20px; font-weight: 800; letter-spacing: 2px; text-transform: uppercase;">Guia Brechó</div>
            </div>

            <div style="padding: 40px; color: #2d3748; line-height: 1.6;">
              <h2 style="margin-top: 0; color: #1a202c; font-size: 22px;">Esqueceu sua senha?</h2>
              <p style="font-size: 16px;">Não se preocupe, isso acontece com todo mundo! Recebemos um pedido para redefinir a senha da sua conta no <strong>Guia Brechó</strong>.</p>
              
              <div style="text-align: center; margin: 40px 0;">
                <a href="%s" style="background-color: #007bff; color: #ffffff; padding: 18px 36px; text-decoration: none; border-radius: 10px; font-weight: bold; display: inline-block; box-shadow: 0 4px 12px rgba(0,123,255,0.3);">Redefinir Senha</a>
              </div>
              
              <p style="font-size: 14px; color: #718096; background: #f7fafc; padding: 15px; border-radius: 8px; border-left: 4px solid #007bff;">
                <strong>Atenção:</strong> Por segurança, este link é válido por apenas <span style="color: #e53e3e; font-weight: bold;">%d minutos</span>. Caso não tenha solicitado a redefinição, você pode ignorar este e-mail com segurança.
              </p>
              
              <div style="margin-top: 40px; border-top: 1px solid #edf2f7; padding-top: 25px;">
                <p style="margin: 0; font-size: 16px; color: #4a5568;">Até logo,<br><strong>Equipe Guia Brechó</strong></p>
              </div>
            </div>

            <div style="background: #f8fafc; color: #a0aec0; padding: 30px; text-align: center; font-size: 12px;">
              &copy; 2026 Guia Brechó. Todos os direitos reservados.<br>
              O melhor caminho para os seus próximos garimpos.
            </div>
          </div>
        </body>
        </html>
        """.formatted(resetLink, ttlMinutes);
  }


}

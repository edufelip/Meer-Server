package com.edufelip.meer.service;

import com.edufelip.meer.domain.auth.PasswordResetNotifier;
import com.edufelip.meer.security.PasswordResetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    String from = properties.getFrom();
    if (from == null || from.isBlank()) {
      from = fallbackFrom;
    }
    if (from != null && !from.isBlank()) {
      message.setFrom(from);
    }
    String subject = properties.getSubject();
    message.setSubject(subject == null || subject.isBlank() ? "Reset your password" : subject);
    message.setText(buildBody(resetLink, ttlMinutes));
    try {
      mailSender.send(message);
    } catch (Exception ex) {
      log.warn("Failed to send password reset email to {}", email, ex);
    }
  }

  private String buildBody(String resetLink, long ttlMinutes) {
    StringBuilder body = new StringBuilder();
    body.append("Use this link to reset your password:\n\n");
    body.append(resetLink);
    body.append("\n\nThis link expires in ").append(ttlMinutes).append(" minutes.");
    return body.toString();
  }
}

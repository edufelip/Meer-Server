package com.edufelip.meer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edufelip.meer.security.PasswordResetProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailSenderTest {

  @Mock private JavaMailSender mailSender;
  @Mock private PasswordResetProperties properties;

  private PasswordResetEmailSender underTest;

  @BeforeEach
  void setUp() {
    underTest = new PasswordResetEmailSender(mailSender, properties, "fallback@example.com");
  }

  @Test
  void sendResetLink_shouldSendEmail_whenEmailIsValid() {
    // Default properties
    when(properties.getFrom()).thenReturn("noreply@example.com");
    when(properties.getSubject()).thenReturn("Reset Password");
    when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

    String email = "user@example.com";
    String resetLink = "http://example.com/reset?token=123";
    long ttlMinutes = 30;

    underTest.sendResetLink(email, resetLink, ttlMinutes);

    verify(mailSender).send(any(MimeMessage.class));
  }

  @Test
  void sendResetLink_shouldNotSendEmail_whenEmailIsNull() {
    underTest.sendResetLink(null, "link", 30);
    verifyNoInteractions(mailSender);
  }

  @Test
  void sendResetLink_shouldNotSendEmail_whenEmailIsBlank() {
    underTest.sendResetLink("", "link", 30);
    verifyNoInteractions(mailSender);
  }

  @Test
  void sendResetLink_shouldLogWarning_whenMailSenderFails() {
    when(properties.getFrom()).thenReturn("noreply@example.com");
    when(properties.getSubject()).thenReturn("Reset Password");
    when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

    // This test ensures the exception is swallowed and logged, as per current implementation
    String email = "user@example.com";
    doThrow(new MailSendException("Failed")).when(mailSender).send(any(MimeMessage.class));

    underTest.sendResetLink(email, "link", 30);

    // Verify method was called but no exception was thrown out of sendResetLink
    verify(mailSender).send(any(MimeMessage.class));
  }
}

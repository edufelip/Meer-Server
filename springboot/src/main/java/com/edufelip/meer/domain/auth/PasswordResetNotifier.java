package com.edufelip.meer.domain.auth;

public interface PasswordResetNotifier {
  void sendResetLink(String email, String resetLink, long ttlMinutes);
}

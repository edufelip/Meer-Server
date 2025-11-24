package com.edufelip.meer.domain.auth;

import com.edufelip.meer.domain.repo.AuthUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ForgotPasswordUseCase {
    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordUseCase.class);
    private final AuthUserRepository authUserRepository;

    public ForgotPasswordUseCase(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    public void execute(String email) {
        var user = authUserRepository.findByEmail(email);
        if (user == null) {
            return; // avoid enumeration
        }
        String token = UUID.randomUUID().toString();
        log.info("Generated password reset token for user {}: {}", user.getEmail(), token);
    }
}

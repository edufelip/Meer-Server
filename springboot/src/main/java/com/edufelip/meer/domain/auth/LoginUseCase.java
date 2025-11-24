package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LoginUseCase {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public LoginUseCase(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResult execute(String email, String password) {
        AuthUser user = authUserRepository.findByEmail(email);
        if (user == null) throw new InvalidCredentialsException();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) throw new InvalidCredentialsException();

        String access = tokenProvider.generateAccessToken(user);
        String refresh = tokenProvider.generateRefreshToken(user);

        AuthenticatedUser authUser = new AuthenticatedUser(user.getId(), user.getDisplayName(), user.getEmail());
        return new AuthResult(access, refresh, authUser);
    }
}

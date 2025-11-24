package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

public class SignupUseCase {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public SignupUseCase(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResult execute(String name, String email, String password) {
        if (authUserRepository.findByEmail(email) != null) throw new EmailAlreadyRegisteredException();

        String hashed = passwordEncoder.encode(password);
        AuthUser user = authUserRepository.save(new AuthUser(email, name, null, hashed));

        String access = tokenProvider.generateAccessToken(user);
        String refresh = tokenProvider.generateRefreshToken(user);

        AuthenticatedUser authUser = new AuthenticatedUser(user.getId(), user.getDisplayName(), user.getEmail());
        return new AuthResult(access, refresh, authUser);
    }
}

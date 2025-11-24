package com.edufelip.meer.domain.auth;

import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.token.InvalidRefreshTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;

public class RefreshTokenUseCase {
    private final TokenProvider tokenProvider;
    private final AuthUserRepository authUserRepository;

    public RefreshTokenUseCase(TokenProvider tokenProvider, AuthUserRepository authUserRepository) {
        this.tokenProvider = tokenProvider;
        this.authUserRepository = authUserRepository;
    }

    public AuthResult execute(String refreshToken) {
        TokenPayload payload;
        try {
            payload = tokenProvider.parseRefreshToken(refreshToken);
        } catch (RuntimeException ex) {
            throw new InvalidRefreshTokenException();
        }

        var user = authUserRepository.findById(payload.getUserId()).orElseThrow(InvalidRefreshTokenException::new);

        String access = tokenProvider.generateAccessToken(user);
        String refresh = tokenProvider.generateRefreshToken(user);

        AuthenticatedUser authUser = new AuthenticatedUser(user.getId(), user.getDisplayName(), user.getEmail());
        return new AuthResult(access, refresh, authUser);
    }
}

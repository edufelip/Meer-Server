package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.token.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class AppleLoginUseCase {
    private final AuthUserRepository authUserRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper = new ObjectMapper();

    public AppleLoginUseCase(AuthUserRepository authUserRepository, TokenProvider tokenProvider, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResult execute(String idToken, String authorizationCode, String client) {
        if (!"ios".equalsIgnoreCase(client)) throw new InvalidAppleTokenException();
        Map<String, Object> payload = decodePayload(idToken);

        String email = (String) payload.get("email");
        if (email == null) throw new InvalidAppleTokenException();
        String name = (String) payload.getOrDefault("name", email);

        String hashed = passwordEncoder.encode(UUID.randomUUID().toString());
        AuthUser user = authUserRepository.findByEmail(email);
        if (user == null) {
            user = authUserRepository.save(new AuthUser(email, name, null, hashed));
        }

        String access = tokenProvider.generateAccessToken(user);
        String refresh = tokenProvider.generateRefreshToken(user);
        AuthenticatedUser authUser = new AuthenticatedUser(user.getId(), user.getDisplayName(), user.getEmail());
        return new AuthResult(access, refresh, authUser);
    }

    private Map<String, Object> decodePayload(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) throw new InvalidAppleTokenException();
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        try {
            return mapper.readValue(decoded, Map.class);
        } catch (Exception e) {
            throw new InvalidAppleTokenException();
        }
    }
}

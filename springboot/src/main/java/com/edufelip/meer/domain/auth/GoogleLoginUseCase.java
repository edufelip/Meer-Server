package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.GoogleClientProperties;
import com.edufelip.meer.security.token.TokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;

public class GoogleLoginUseCase {
    private final AuthUserRepository authUserRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final GoogleClientProperties googleClientProperties;

    private final NetHttpTransport transport = new NetHttpTransport();
    private final JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    public GoogleLoginUseCase(AuthUserRepository authUserRepository,
                              TokenProvider tokenProvider,
                              PasswordEncoder passwordEncoder,
                              GoogleClientProperties googleClientProperties) {
        this.authUserRepository = authUserRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.googleClientProperties = googleClientProperties;
    }

    public AuthResult execute(String idToken, String client) {
        String clientId = switch (client.toLowerCase()) {
            case "android" -> googleClientProperties.getAndroidClientId();
            case "ios" -> googleClientProperties.getIosClientId();
            case "web" -> googleClientProperties.getWebClientId();
            default -> throw new InvalidGoogleTokenException();
        };
        if (clientId == null || clientId.isBlank()) throw new InvalidGoogleTokenException();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidGoogleTokenException();
        }
        if (token == null) throw new InvalidGoogleTokenException();

        var payload = token.getPayload();
        String email = payload.getEmail();
        if (email == null) throw new InvalidGoogleTokenException();
        String name = (String) payload.get("name");
        if (name == null) name = email;
        String picture = (String) payload.get("picture");

        String hashedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        AuthUser user = authUserRepository.findByEmail(email);
        if (user == null) {
            user = authUserRepository.save(new AuthUser(email, name, picture, hashedPassword));
        }

        String access = tokenProvider.generateAccessToken(user);
        String refresh = tokenProvider.generateRefreshToken(user);
        AuthenticatedUser authUser = new AuthenticatedUser(user.getId(), user.getDisplayName(), user.getEmail());
        return new AuthResult(access, refresh, authUser);
    }
}

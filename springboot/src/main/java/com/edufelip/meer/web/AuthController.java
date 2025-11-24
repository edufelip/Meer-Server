package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.auth.*;
import com.edufelip.meer.dto.AuthDtos;
import com.edufelip.meer.mapper.AuthMappers;
import com.edufelip.meer.security.token.InvalidRefreshTokenException;
import com.edufelip.meer.security.token.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final SignupUseCase signupUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final AppleLoginUseCase appleLoginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final AuthUserRepository authUserRepository;

    public AuthController(LoginUseCase loginUseCase,
                          SignupUseCase signupUseCase,
                          GoogleLoginUseCase googleLoginUseCase,
                          AppleLoginUseCase appleLoginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          ForgotPasswordUseCase forgotPasswordUseCase,
                          AuthUserRepository authUserRepository) {
        this.loginUseCase = loginUseCase;
        this.signupUseCase = signupUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.appleLoginUseCase = appleLoginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.authUserRepository = authUserRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@RequestBody AuthDtos.LoginRequest body) {
        var result = loginUseCase.execute(body.email(), body.password());
        return ResponseEntity.ok(new AuthDtos.AuthResponse(result.getToken(), result.getRefreshToken(), AuthMappers.toDto(result.getUser())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(@RequestBody AuthDtos.RefreshRequest body) {
        var result = refreshTokenUseCase.execute(body.refreshToken());
        return ResponseEntity.ok(new AuthDtos.AuthResponse(result.getToken(), result.getRefreshToken(), AuthMappers.toDto(result.getUser())));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody AuthDtos.ForgotPasswordRequest body) {
        forgotPasswordUseCase.execute(body.email());
        return ResponseEntity.ok(Map.of("message", "Reset email sent"));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthDtos.AuthResponse> loginWithGoogle(@RequestBody AuthDtos.GoogleLoginRequest body,
                                                                 @RequestHeader(name = "Authorization", required = false) String authHeader) {
        String idToken = body.idToken();
        if ((idToken == null || idToken.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            idToken = authHeader.substring("Bearer ".length()).trim();
        }
        if (idToken == null || idToken.isBlank()) throw new InvalidGoogleTokenException();
        var result = googleLoginUseCase.execute(idToken, body.client());
        return ResponseEntity.ok(new AuthDtos.AuthResponse(result.getToken(), result.getRefreshToken(), AuthMappers.toDto(result.getUser())));
    }

    @PostMapping("/apple")
    public ResponseEntity<AuthDtos.AuthResponse> loginWithApple(@RequestBody AuthDtos.AppleLoginRequest body) {
        if (!"apple".equalsIgnoreCase(body.provider())) throw new InvalidAppleTokenException();
        var result = appleLoginUseCase.execute(body.idToken(), body.authorizationCode(), body.client());
        return ResponseEntity.ok(new AuthDtos.AuthResponse(result.getToken(), result.getRefreshToken(), AuthMappers.toDto(result.getUser())));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthDtos.AuthResponse> signup(@RequestBody AuthDtos.SignupRequest body) {
        var result = signupUseCase.execute(body.name(), body.email(), body.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthDtos.AuthResponse(result.getToken(), result.getRefreshToken(), AuthMappers.toDto(result.getUser())));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader("Authorization") String authHeader,
                                                  RefreshTokenUseCase refreshTokenUseCase) {
        // me still parsed via token; use LoginUseCase’s tokenProvider in Java migration? For parity keep stubbed with invalid token
        throw new InvalidTokenException();
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>> handleEmailTaken(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({InvalidGoogleTokenException.class, InvalidAppleTokenException.class, InvalidTokenException.class, InvalidRefreshTokenException.class})
    public ResponseEntity<Map<String, String>> handleInvalidToken(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }
}

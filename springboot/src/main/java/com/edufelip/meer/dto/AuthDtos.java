package com.edufelip.meer.dto;

public class AuthDtos {
    public record LoginRequest(String email, String password) {}
    public record SignupRequest(String name, String email, String password) {}
    public record GoogleLoginRequest(String client, String idToken) {}
    public record AppleLoginRequest(String provider, String idToken, String authorizationCode, String client) {}
    public record RefreshRequest(String refreshToken) {}
    public record ForgotPasswordRequest(String email) {}

    public record UserDto(String id, String name, String email) {}

    public record AuthResponse(String token, String refreshToken, UserDto user) {}
}

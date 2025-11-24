package com.edufelip.meer.security.token;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() { super("Invalid or expired token"); }
}

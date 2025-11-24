package com.edufelip.meer.domain.auth;

public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException() { super("Invalid or expired token"); }
}

package com.edufelip.meer.domain.auth;

public class InvalidAppleTokenException extends RuntimeException {
    public InvalidAppleTokenException() { super("Invalid or expired token"); }
}

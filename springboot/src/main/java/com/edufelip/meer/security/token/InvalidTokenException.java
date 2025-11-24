package com.edufelip.meer.security.token;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() { super("Invalid or expired token"); }
}

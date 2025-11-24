package com.edufelip.meer.domain.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() { super("Email already registered"); }
}

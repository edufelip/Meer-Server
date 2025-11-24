package com.edufelip.meer.security.guards;

import com.edufelip.meer.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;

public class FirebaseAuthGuard {
    public static final String AUTH_HEADER = "Authorization";
    private final SecurityProperties props;

    public FirebaseAuthGuard(SecurityProperties props) {
        this.props = props;
    }

    public void validate(HttpServletRequest request) {
        if (props.isDisableAuth()) return;
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new GuardException("Missing or invalid " + AUTH_HEADER + " header");
        }
        // TODO: add Firebase verification
    }
}

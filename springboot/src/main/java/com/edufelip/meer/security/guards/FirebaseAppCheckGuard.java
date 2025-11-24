package com.edufelip.meer.security.guards;

import com.edufelip.meer.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;

public class FirebaseAppCheckGuard {
    public static final String APP_CHECK_HEADER = "X-Firebase-AppCheck";
    private final SecurityProperties props;

    public FirebaseAppCheckGuard(SecurityProperties props) {
        this.props = props;
    }

    public void validate(HttpServletRequest request) {
        if (!props.isRequireAppCheck() || props.isDisableAuth()) return;
        String header = request.getHeader(APP_CHECK_HEADER);
        if (header == null || header.isBlank()) {
            throw new GuardException("Missing " + APP_CHECK_HEADER + " header");
        }
        // TODO: plug real Firebase App Check validation
    }
}

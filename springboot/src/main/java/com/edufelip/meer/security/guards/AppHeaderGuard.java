package com.edufelip.meer.security.guards;

import com.edufelip.meer.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;

public class AppHeaderGuard {
    public static final String APP_HEADER = "X-App-Package";
    private final SecurityProperties props;

    public AppHeaderGuard(SecurityProperties props) {
        this.props = props;
    }

    public void validate(HttpServletRequest request) {
        if (!props.isRequireAppHeader()) return;
        String header = request.getHeader(APP_HEADER);
        if (header == null || !header.equals(props.getAppPackage())) {
            throw new GuardException("Missing or invalid " + APP_HEADER + " header");
        }
    }
}

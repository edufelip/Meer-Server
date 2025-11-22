package com.edufelip.meer.security.guards

import com.edufelip.meer.security.SecurityProperties
import jakarta.servlet.http.HttpServletRequest

class AppHeaderGuard(private val props: SecurityProperties) {
    fun validate(request: HttpServletRequest) {
        if (!props.requireAppHeader) return
        val header = request.getHeader(APP_HEADER)
        if (header == null || header != props.appPackage) {
            throw GuardException("Missing or invalid $APP_HEADER header")
        }
    }

    companion object {
        const val APP_HEADER = "X-App-Package"
    }
}

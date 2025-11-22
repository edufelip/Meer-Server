package com.edufelip.meer.security.guards

import com.edufelip.meer.security.SecurityProperties
import jakarta.servlet.http.HttpServletRequest

class FirebaseAppCheckGuard(private val props: SecurityProperties) {
    fun validate(request: HttpServletRequest) {
        if (!props.requireAppCheck || props.disableAuth) return
        val header = request.getHeader(APP_CHECK_HEADER)
        if (header.isNullOrBlank()) {
            throw GuardException("Missing $APP_CHECK_HEADER header")
        }
        // NOTE: Stub — real Firebase App Check token verification should be added when credentials are available.
    }

    companion object {
        const val APP_CHECK_HEADER = "X-Firebase-AppCheck"
    }
}

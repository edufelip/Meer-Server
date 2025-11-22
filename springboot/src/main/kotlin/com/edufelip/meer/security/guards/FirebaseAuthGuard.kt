package com.edufelip.meer.security.guards

import com.edufelip.meer.security.SecurityProperties
import jakarta.servlet.http.HttpServletRequest

class FirebaseAuthGuard(private val props: SecurityProperties) {
    fun validate(request: HttpServletRequest) {
        if (props.disableAuth) return
        val header = request.getHeader(AUTH_HEADER)
        if (header.isNullOrBlank() || !header.startsWith("Bearer ")) {
            throw GuardException("Missing or invalid $AUTH_HEADER header")
        }
        // NOTE: Stub — plug in Firebase Admin SDK verification when credentials are configured.
    }

    companion object {
        const val AUTH_HEADER = "Authorization"
    }
}

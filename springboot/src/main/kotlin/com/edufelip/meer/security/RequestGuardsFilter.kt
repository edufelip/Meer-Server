package com.edufelip.meer.security

import com.edufelip.meer.security.guards.AppHeaderGuard
import com.edufelip.meer.security.guards.FirebaseAppCheckGuard
import com.edufelip.meer.security.guards.FirebaseAuthGuard
import com.edufelip.meer.security.guards.GuardException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class RequestGuardsFilter(
    private val securityProps: SecurityProperties
) : OncePerRequestFilter() {

    private val appHeaderGuard = AppHeaderGuard(securityProps)
    private val appCheckGuard = FirebaseAppCheckGuard(securityProps)
    private val authGuard = FirebaseAuthGuard(securityProps)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            appHeaderGuard.validate(request)
            appCheckGuard.validate(request)
            authGuard.validate(request)
        } catch (ex: GuardException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.message)
            return
        }

        filterChain.doFilter(request, response)
    }
}

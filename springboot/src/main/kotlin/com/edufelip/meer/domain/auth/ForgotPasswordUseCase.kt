package com.edufelip.meer.domain.auth

import com.edufelip.meer.domain.AuthUserRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class ForgotPasswordUseCase(
    private val authUserRepository: AuthUserRepository
) {
    private val log = LoggerFactory.getLogger(ForgotPasswordUseCase::class.java)

    fun execute(email: String) {
        val user = authUserRepository.findByEmail(email)
        if (user == null) {
            // Avoid enumeration: respond success even if the email is not registered.
            return
        }

        val resetToken = UUID.randomUUID().toString()
        // TODO: Persist reset token and send email. For now, log for visibility during development.
        log.info("Generated password reset token for user {}: {}", user.email, resetToken)
    }
}

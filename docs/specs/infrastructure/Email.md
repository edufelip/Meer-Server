# Email Service Specification

## Purpose
The Email Service module manages automated outbound communication from the Meer platform, primarily used for account security and user lifecycle notifications.

## Scope
- Password reset notifications.
- HTML email templating.
- SMTP integration.

## Password Reset Workflow
When a user requests a password reset (`POST /auth/forgot-password`):
1. **Token Generation**: A unique UUID token is generated and stored with a TTL (Time-To-Live).
2. **Notification**: The `PasswordResetEmailSender` is invoked.
3. **Email Delivery**: An HTML email is sent via SMTP.

### Email Template
- **Subject**: "Recuperação de Senha - Guia Brechó" (or similar localized text).
- **Body**: Includes a call-to-action button linking to the frontend reset page.
- **Safety**: Includes a warning about token expiration and instruction to ignore if the request wasn't initiated by the user.

## Configuration
- **Provider**: Uses `spring-boot-starter-mail`.
- **Infrastructure**: Configured via standard `spring.mail.*` properties.
- **Link Generation**: The reset link base URL is provided via `SECURITY_PASSWORD_RESET_LINK_BASE_URL`.

## Invariants
- Emails must be sent asynchronously or in a way that doesn't block the API response (Current implementation is synchronous within the use case, which is a known implementation detail to be aware of).
- All templates must be mobile-responsive and provide a plain-text fallback (Implicit goal).

## Non-Goals
- Marketing newsletters (Handled by external providers).
- Inbound email processing.

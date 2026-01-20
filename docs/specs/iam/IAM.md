# Identity and Access Management (IAM) Specification

## Purpose
The Identity and Access Management (IAM) module manages user lifecycle, authentication, authorization, and profile management for the Meer platform.

## Scope
- User registration and login (local and social)
- Token-based session management (JWT)
- Role-based access control (RBAC)
- Profile and account settings
- Security measures (Rate limiting, sanitization, guards)

## Definitions
- **AuthUser**: Core domain entity representing a registered user.
- **Access Token**: Short-lived JWT used for authorizing API requests.
- **Refresh Token**: Long-lived JWT used to obtain new access tokens.
- **Social Login**: Authentication via third-party providers (Google, Apple).
- **Role**: Level of permission (USER or ADMIN).

## User Model
| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| id | UUID (v7) | Unique identifier | Primary Key |
| email | String | User's email address | Unique, Nullable (except local) |
| displayName | String | Public name | Required |
| passwordHash | String | Bcrypt hashed password | Required for local auth |
| role | Enum | USER or ADMIN | Default: USER |
| photoUrl | String | URL to profile avatar | Optional |
| bio | String | User biography | Max 1000 chars |
| notifyNewStores| Boolean | Notification preference | Default: true |
| notifyPromos | Boolean | Notification preference | Default: true |

## Authentication Flows

### Local Authentication
1. **Signup**: `POST /auth/signup`
   - Inputs: `name`, `email`, `password`
   - Behavior: Creates `AuthUser`, hashes password, returns tokens.
   - Errors: `409 Conflict` if email exists.
2. **Login**: `POST /auth/login`
   - Inputs: `email`, `password`
   - Behavior: Validates credentials, returns tokens.
   - Errors: `401 Unauthorized` for invalid credentials.
3. **Dashboard Login**: `POST /dashboard/login`
   - Inputs: `email`, `password`
   - Behavior: Identical to standard login but enforces the `ADMIN` role.
   - Errors: `403 Forbidden` if credentials are valid but the user is not an admin.

### Social Authentication
1. **Google Login**: `POST /auth/google`
   - Inputs: `idToken`, `client`
   - Behavior: Validates Google token, creates/updates user, returns tokens.
2. **Apple Login**: `POST /auth/apple`
   - Inputs: `idToken`, `authorizationCode`, `client`
   - Behavior: Validates Apple identity, creates/updates user, returns tokens.

### Token Management
- **Refresh Token**: `POST /auth/refresh`
  - Inputs: `refreshToken`
  - Behavior: Validates refresh token, returns new access and refresh tokens.
- **Token Invalidation**: Handled via expiration and client-side deletion.

### Password Recovery
1. **Forgot Password**: `POST /auth/forgot-password`
   - Inputs: `email`
   - Behavior: Generates a temporary token, sends email with reset link.
2. **Reset Password**: `POST /auth/reset-password`
   - Inputs: `token`, `password`
   - Behavior: Validates token, updates `passwordHash`.

## Profile Management
- **Get Profile**: `GET /profile` or `GET /auth/me`
  - Requires: Bearer Token
  - Output: `ProfileDto`
- **Update Profile**: `PUT /profile` or `PATCH /profile`
  - Allows updating `name`, `avatarUrl`, `bio`, and notification preferences.
  - **Avatar Upload**: Multi-step process:
    1. `POST /profile/avatar/upload` to get a signed GCS URL.
    2. Client uploads image to GCS.
    3. Client sends public URL to profile update endpoint.
  - **Moderation**: New `avatarUrl` is automatically enqueued for NSFW moderation.

## Account Lifecycle
- **Delete Account**: `DELETE /account`
  - Inputs: `email` (confirmation)
  - Behavior: Verifies email, triggers `DeleteUserUseCase`.
  - Side Effects: Deletes favorites, clears owned store links, notifies systems.

## Security Controls
1. **JWT Security**:
   - RS256/HS256 signed.
   - Claims: `sub` (userId), `email`, `name`, `role`.
2. **Rate Limiting**: Applied via `RateLimitFilter` to prevent brute force.
3. **Request Sanitization**: All incoming strings are sanitized via `SanitizingRequestFilter` to prevent XSS.
4. **Header Guards**: `AppHeaderGuard` may enforce specific client headers (implementation detail).
### Dashboard Access
`DashboardAdminGuardFilter` ensures only users with `ADMIN` role access `/dashboard/**` routes.

## Security Guards
In addition to role-based filters, the system employs dedicated guards to validate request integrity.

### 1. App Header Guard (`AppHeaderGuard`)
- **Header**: `X-App-Package`
- **Purpose**: Restricts API consumption to authorized mobile clients by verifying a specific package identifier.
- **Toggle**: Can be enabled/disabled via `meer.security.require-app-header`.

### 2. Authentication Guard (`FirebaseAuthGuard`)
- **Header**: `Authorization: Bearer <token>`
- **Purpose**: Validates that the provided JWT is valid, not expired, and corresponds to an existing user in the database.
- **Toggle**: Can be disabled globally for testing via `meer.security.disable-auth`.

## Error Handling
- `401 Unauthorized`: Invalid or expired tokens, bad credentials, or missing security headers.
- `403 Forbidden`: Insufficient role for the requested resource.

## Invariants
- Every authenticated request must have a valid `id` (userId) in the token sub claim.
- Admins have all user permissions plus dashboard access.
- Social login users do not have a password hash (unless they set one later).

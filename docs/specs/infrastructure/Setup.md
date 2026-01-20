# Setup and Environment Variables Specification

## Purpose
This document provides a comprehensive list of all environment variables and secrets required to configure and run the Meer platform across different environments.

## Core Platform Variables

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profiles (comma-separated) | `prod`, `local-db`, `local` |
| `DB_HOST` | Database host address | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `meer_prod` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | (Secret) |

## Security and Identity

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `SECURITY_JWT_SECRET` | JWT Signing Key (Min 32 bytes) | (Secret) |
| `SECURITY_JWT_ACCESS_TTL_MINUTES` | Access token expiration | `60` |
| `SECURITY_JWT_REFRESH_TTL_DAYS` | Refresh token expiration | `7` |
| `GOOGLE_ANDROID_CLIENT_ID` | OAuth Client ID for Android | (Secret) |
| `GOOGLE_IOS_CLIENT_ID` | OAuth Client ID for iOS | (Secret) |
| `GOOGLE_WEB_CLIENT_ID` | OAuth Client ID for Web/Dashboard | (Secret) |
| `MEER_CORS_ALLOWED_ORIGINS` | Allowed CORS origins (regex/patterns) | `https://*.guiabrecho.com.br` |

## Storage (Google Cloud Storage)

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `GCS_BUCKET` | Destination GCS bucket name | `meer-uploads` |
| `GCS_PUBLIC_BASE_URL` | Public viewing URL for assets | `https://storage.googleapis.com/...` |
| `GCS_SIGNED_URL_TTL_MINUTES` | TTL for upload signed URLs | `120` |
| `GCS_AVATARS_PREFIX` | Prefix/Folder for user avatars | `avatars` |

## Moderation (AI)

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `NSFW_MODEL_PATH` | Path to the ONNX model file | `classpath:models/nsfw/...` |
| `NSFW_MODERATION_ENABLED` | Global toggle for AI scanning | `true` |
| `NSFW_THRESHOLD_ALLOW` | Max score for auto-approval | `0.30` |
| `NSFW_THRESHOLD_REVIEW` | Min score for auto-blocking | `0.70` |

## Notifications (Firebase)

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `FIREBASE_ENABLED` | Global toggle for FCM delivery | `false` |
| `FIREBASE_PROJECT_ID` | Google Cloud Project ID | (Secret) |
| `FIREBASE_CREDENTIALS_PATH` | Local path to service account JSON | `/opt/meer-prod/...` |

## Infrastructure & Maintenance

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `SPRING_MAIL_HOST` | SMTP server host | `smtp.gmail.com` |
| `SPRING_MAIL_USERNAME` | SMTP username | (Secret) |
| `SPRING_MAIL_PASSWORD` | SMTP password | (Secret) |
| `ASSET_DELETION_WORKER_DELAY_MS`| Interval for asset cleanup job | `30000` |

## DevOps & Deployment (Secrets)
These are typically stored in GitHub Actions Secrets:
- `SSH_HOST`, `SSH_USER`, `SSH_KEY`: For production server access.
- `PROD_ENV_FILE_B64`: Base64 encoded version of the production `.env` file.
- `FIREBASE_SERVICE_ACCOUNT_JSON_B64`: Base64 encoded service account file.

## Invariants
- `SECURITY_JWT_SECRET` must be at least 256 bits (32 bytes).
- All binary assets must be hosted under the same `GCS_BUCKET` for the `deriveKey` logic to function.
- `FIREBASE_CREDENTIALS_PATH` must point to a readable JSON file if `FIREBASE_ENABLED` is `true`.

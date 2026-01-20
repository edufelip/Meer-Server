# API Observability and Logging Specification

## Purpose
The Observability module provides visibility into the system's operational state through structured HTTP request/response logging, enabling debugging and audit capabilities without compromising user privacy.

## Scope
- HTTP traffic logging.
- PII and Credential masking.
- Log volume management (truncation).

## Request/Response Logging
The system implements a global `RequestResponseLoggingFilter` that intercepts every API call.

### Log Format
Each log entry includes:
- **Metadata**: HTTP Method, URI, Response Status, Execution Time (ms).
- **Headers**: Key-value pairs of request and response headers.
- **Payloads**: Request and response bodies (where applicable).

### Security and Privacy (Masking)
To comply with security best practices and privacy regulations:
- **Authorization Header**: The value of the `Authorization` header is always replaced with `***masked***`.
- **Sensitive Paths**: Body logging is completely skipped for the following sensitive paths:
  - `/auth/**` (Credentials, Tokens)
  - `/profile/**` (Personal info)
  - `/dashboard/login` (Admin credentials)
  - `/support/**` (User messages)
  - `/uploads/**` (Binary data)

### Robustness (Truncation)
To prevent log bloat and performance degradation:
- **Size Limit**: Payloads are truncated at **4000 bytes** per body.
- **Binary Filtering**: `multipart/form-data` bodies are never logged.

## Technical Implementation
- Uses `ContentCachingRequestWrapper` and `ContentCachingResponseWrapper` to read streams multiple times without exhausting them.
- Logged via SLF4J (Logback) at the `INFO` level.

## Invariants
- Logging must never interfere with the primary request/response flow.
- The `copyBodyToResponse()` call must be executed in the `finally` block to ensure the client receives the data even if logging fails.

# Infrastructure and Storage Specification

## Purpose
This document specifies the foundational technical services of the Meer platform, including binary storage, caching, data persistence, and global error handling.

## Scope
- Binary storage (Google Cloud Storage)
- In-memory caching (Caffeine)
- Data persistence and schema management (Postgres/PostGIS/Flyway)
- Centralized error handling

## Binary Storage (GCS)
The system uses Google Cloud Storage (GCS) as the primary store for user-uploaded media.

### Abstraction: `PhotoStoragePort`
All storage operations are performed through this port to decouple the domain from GCS specifics.

### Upload Workflow (Signed URLs)
1. **Request**: The client requests an upload slot for an entity (avatar, store photo, or guide image).
2. **Signed URL**: The server generates a GCS V4 Signed URL with `PUT` permission and a specific expiration (default: 120 minutes).
3. **Upload**: The client uploads the binary directly to GCS using the signed URL.
4. **Registration**: The client provides the GCS `fileKey` back to the server in subsequent domain operations.

### Storage Paths
- **Avatars**: `avatars/{userId}-{uuid}`
- **Store Photos**: `stores/{storeId}/{uuid}`
- **Guide Content (with store)**: `stores/{storeId}/{uuid}`
- **Guide Content (no store)**: `contents/{uuid}`

### Cleanup
The system supports both URL-based and key-based deletion. It includes a fallback to delete local files if URLs start with `/uploads/` (used for development environments).

## Local Static Assets and Legacy Uploads
While GCS is the primary storage for production, the system supports a local `/uploads/**` directory for development and specific legacy/development workflows.

### Static File Serving
- **Path**: `/uploads/**`
- **Location**: Maps to the local `uploads/` directory in the project root.
- **Config**: Managed via `StaticResourceConfig`.

### Multipart Uploads (Legacy/Development): `/uploads`
- **Endpoints**: `POST /uploads/communities`, `POST /uploads/posts`.
- **Behavior**: Accepts multipart file data and JSON metadata.
- **Security**: Sanitizes incoming JSON strings using Jsoup to prevent XSS. 
- **Note**: These endpoints are largely decoupled from the main GCS-based flows and serve specific legacy or internal upload needs.

## Caching (Caffeine)
In-memory caching is used to reduce database load and improve response times for frequently accessed, semi-static data.

| Cache Name | Max Size | TTL (expireAfterWrite) | Use Case |
|------------|----------|------------------------|----------|
| `featuredTop10` | 10 | 10 minutes | Landing page featured items |
| `guideTop10` | 10 | 10 minutes | Popular guide articles |
| `storeRatings` | 200 | 5 minutes | Denormalized store rating data |
| `categoriesAll` | 5 | 60 minutes | Full category list |

## Rate Limiting
The system enforces operation-specific rate limits to prevent spam and abuse, managed via `RateLimitService`.

| Operation | Scope | Limit | Window |
|-----------|-------|-------|--------|
| Comment Creation | Per User | 10 | 1 Minute |
| Comment Editing | Per User | 20 | 1 Minute |
| Like Actions | Per User | 60 | 1 Minute |
| Support Contact | Per Client IP| 3 | 1 Hour |

## Cross-Origin Resource Sharing (CORS)
CORS is configured to allow web clients to interact with the API securely.

- **Allowed Methods**: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- **Allowed Headers**: All (`*`).
- **Allowed Origins**: 
  - Controlled via `MEER_CORS_ALLOWED_ORIGINS` environment variable.
  - Supports wildcard patterns (e.g., `https://*.guiabrecho.com.br`).
- **Credentials**: Only allowed if explicit origins are provided (not `*`).

## Data Persistence
- **Database**: PostgreSQL 15+
- **Extensions**: `PostGIS` (used for nearby store searches and geographic coordinates).
- **Primary Keys**: UUID v7 (time-ordered UUIDs) are preferred for performance and sortability.
- **Migrations**: Managed via `Flyway`. All schema changes must be scripted in `src/main/resources/db/migration`.

## Error Handling
The `RestExceptionHandler` provides a uniform error response format:
```json
{
  "message": "Human readable error description"
}
```

### Handled Scenarios
- **Auth Errors**: `401 Unauthorized` for invalid tokens/refresh tokens.
- **Validation**: `400 Bad Request` for malformed JSON, type mismatches, or `@Valid` constraint violations.
- **Upload Limits**: `400 Bad Request` for `MaxUploadSizeExceededException`.
- **Domain Errors**: `ResponseStatusException` used within use cases is translated to its respective status and reason.
- **Fallback**: `500 Internal Server Error` with logging for any unhandled exceptions.

## Invariants
- Direct access to GCS binaries should be done via signed URLs for uploads and public URLs for viewing.
- Database primary keys (UUID) are generated on the server using `Uuid7` generator if not provided.
- XSS prevention is enforced globally via `SanitizingRequestFilter`.

# Data Validation and Environmental Configuration Specification

## Purpose
This specification documents the implicit data normalization rules, cross-field validation logic, and environmental profile behaviors that govern the Meer platform's runtime.

## Data Normalization Rules

### Store Categories
To ensure search consistency and prevent taxonomy fragmentation, the system applies the following rules to all store categories via `StoreCategoryNormalizer`:
- **Case Insensitivity**: All category labels are converted to lowercase.
- **Sanitization**: Whitespace is trimmed from both ends.
- **Deduplication**: Duplicate categories within a single store are removed.
- **Filtering**: Null or blank category strings are discarded.

### Social Media Validation
The `StoreSocialValidator` enforces the following constraints:
- **Facebook**: Must be a valid HTTP/HTTPS URL.
- **Instagram**: Must be a single word (no whitespace).
- **Website**: Must be a valid URL and must contain the `.com` TLD (Business rule constraint).

## Technical Limits

### File Uploads (Multipart)
Managed via `MultipartConfig`, the system enforces the following hard limits:
- **Max File Size**: 25 MB.
- **Max Request Size**: 75 MB (Total payload).
- **Max File Count**: Increased to **30 files** per request to support batch photo uploads for store registries.

## Key-based Localization Strategy
The system follows a "Backend-as-Reference" localization model:
- **Keys**: The database stores stable identifiers (e.g., `name_string_id: "brecho_de_casa"`).
- **Responsibility**: Frontend clients (Mobile/Web) are responsible for mapping these keys to the appropriate localized strings (i.e., the backend does not perform translation).

## Environmental Configuration
The system behavior changes based on the active Spring Profile:

| Profile | Purpose | Persistence | Flyway Behavior |
|---------|---------|-------------|-----------------|
| `default` | Standard Cloud/Dev | External Postgres | Standard Migration |
| `local-db`| Local Development | Local Postgres | Full Migration |
| `prod` | Production | Managed Postgres | Restricted (No clean) |
| `local` | Sandbox/CI | In-memory H2 | Schema-only |

## Invariants
- Production-like profiles (`default`, `prod`, `local-db`) strictly disable Hibernate DDL-Auto (`ddl-auto: none`) to ensure Flyway remains the sole source of schema truth.
- Security guards are enabled by default in all profiles unless `security.disableAuth` is explicitly set to `true`.

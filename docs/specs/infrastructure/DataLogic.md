# Spatial Engine and Deep Data Logic Specification

## Purpose
This document defines the low-level data processing rules, spatial calculation strategies, and atomic integrity guarantees that govern the Meer platform's database layer.

## Spatial Search Engine
The system provides location-aware store discovery through a dual-mode engine, toggled via `meer.postgis.enabled`.

### 1. High-Precision Mode (PostGIS)
- **Implementation**: Uses native PostgreSQL PostGIS extensions.
- **Formula**: `ST_DistanceSphere(ST_MakePoint(lon1, lat1), ST_MakePoint(lon2, lat2))`.
- **Accuracy**: Accounts for the earth's curvature (Spherical model).
- **Use Case**: Production and Staging environments.

### 2. Fallback/Standard Mode (Euclidean)
- **Implementation**: Pure JDBC/JPQL calculation.
- **Formula**: Simple Pythagorean distance `SQRT(POWER(lat1-lat2, 2) + POWER(lon1-lon2, 2))`.
- **Accuracy**: Planar approximation (only accurate for very small distances).
- **Use Case**: Local development (H2) or environments without PostGIS installed.

## Atomic Data Integrity
To ensure consistency under high concurrency, the system avoids standard "read-modify-save" cycles for counters.

### Atomic Counters
Denormalized counts are updated using atomic database increments:
- **Likes**: `UPDATE guide_content SET like_count = like_count + 1 WHERE id = :id`
- **Comments**: `UPDATE guide_content SET comment_count = comment_count + 1 WHERE id = :id`
- **Invariant**: These counters are never manually set via DTOs to prevent drift.

### Cascading Deletion Logic
When a user is deleted, custom SQL ensures no orphaned references remain:
- **Favorites**: Hard-deleted via native query `DELETE FROM auth_user_favorites`.
- **Reference Nulling**: References in `guide_content` (deleted_by) and `guide_content_comment` (edited_by) are set to `NULL` rather than deleting the content, preserving the history of the platform.

## Advanced Search Logic
The `ThriftStoreRepository.search()` function applies implicit weighting:
- **Priority**: Searches are performed across `name`, `description`, and `neighborhood`.
- **Normalization**: All search terms are trimmed and compared case-insensitively using `LOWER()`.
- **Matching**: Uses `LIKE %term%` for partial match support.

## Invariants
- Geographic coordinates (Latitude/Longitude) are stored as `Double` in the core but treated as `Point` types in spatial queries.
- Pagination is 0-indexed at the repository level but 1-indexed in public discovery APIs.
- The system must explicitly check for PostGIS availability at startup to determine the spatial strategy.

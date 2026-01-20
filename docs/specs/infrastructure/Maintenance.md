# System Maintenance and Automated Jobs Specification

## Purpose
The System Maintenance module ensures data integrity, legal compliance (e.g., Right to be Forgotten), and storage optimization through automated background tasks and scheduled cleanup jobs.

## Scope
- Orphaned and requested asset deletion (GCS cleanup).
- Soft-delete to hard-delete transitions (if applicable).
- Data purging (expired tokens, old logs).

## Asset Deletion Job
When binary assets (images) are requested to be deleted (e.g., store deletion, photo replacement), the system uses a robust background job to ensure the file is physically removed from storage.

### Model: `asset_deletion_job`
| Field | Type | Description |
|-------|------|-------------|
| url | String | Full URL of the asset to delete |
| status | Enum | PENDING, IN_PROGRESS, RETRY, SUCCEEDED, FAILED |
| attempts | Integer | Number of retried attempts |
| next_attempt_at | Instant | Scheduled time for the next retry |
| last_error | String | Error message from the last failure |

### Workflow: `AssetDeletionWorker`
1. **Polling**: The worker runs every 30 seconds (configurable via `asset-deletion.worker.delay-ms`).
2. **Batching**: Processes up to 25 jobs per cycle.
3. **Execution**: Attempts to delete the URL via `PhotoStoragePort`.
4. **Retry Strategy**:
   - Uses **Exponential Backoff**: `baseBackoff * 2^(attempt-1)`.
   - Max attempts: 8.
   - Max backoff: 1 hour.
5. **Finalization**: Jobs are marked as `SUCCEEDED` or `FAILED`.

## Data Cleanup Policies

### 1. Comment Purging
As per `V8__comment_search_indexes_and_cleanup.sql`:
- **Strategy**: Comments are transitioned from soft-delete to hard-delete to comply with storage policies and simplify the schema.
- **Search**: Gin Trigram indexes are used on `guide_content_comment.body` to support efficient administrative searching of content.

### 2. Moderation Cleanup
Managed by `BlockedImageCleanupService` (see Moderation Spec):
- **Target**: Images with `BLOCKED` or `MANUALLY_REJECTED` status.
- **Frequency**: Every 2 minutes.

### 3. Deleted User cleanup
When a user is deleted (`DeleteUserUseCase`):
- **Cascade**: Favorites are cleared.
- **Store Links**: `owned_thrift_store_id` is nulled in `auth_user` before deletion to prevent FK violations.
- **Engagement**: Comments and likes associated with the user are handled based on domain-specific rules (often nulled or deleted).

## Invariants
- Asset deletion is **eventually consistent**. If a storage provider is down, the job will retry until successful or exhausted.
- Maintenance tasks must not block user-facing request threads.
- Deletion logs should be preserved in the `asset_deletion_job` table (even if failed) for audit purposes.

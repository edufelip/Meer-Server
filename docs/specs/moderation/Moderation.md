# Content Moderation Specification

## Purpose
The Content Moderation module ensures that user-uploaded images (avatars, store photos, guide content) comply with safety guidelines by using AI-based NSFW detection and a manual review workflow.

## Scope
- Automated image scanning using ONNX Runtime.
- Background task queue for async processing.
- Manual review dashboard for administrators.
- Automated cleanup of inappropriate content.

## Definitions
- **Inference**: The process of running an AI model on an image to get a probability score.
- **NSFW Score**: A probability from 0.0 (Safe) to 1.0 (Adult Content).
- **Flagged**: Content that falls in a "gray zone" and requires human judgment.

## Moderation Workflow

### 1. Enqueueing
Whenever a user uploads an image URL to their profile, a store, or a guide, the system creates a record in the `image_moderation` table with status `PENDING`.
- **Entity Types**: `STORE_PHOTO`, `USER_AVATAR`, `GUIDE_CONTENT_IMAGE`.

### 2. Processing (Async)
The `ImageModerationWorker` polls for pending records and:
1. Downloads the image from GCS.
2. Preprocesses it (Resize to 224x224, Normalize).
3. Runs the ONNX model (`model_quantized.onnx`).
4. Calculates the NSFW score.

### 3. Automated Policy
- **Score < 0.30**: Automatically `APPROVED`. No further action.
- **0.30 <= Score < 0.70**: Marked as `FLAGGED_FOR_REVIEW`. Visible in Admin Dashboard.
- **Score >= 0.70**: Automatically `BLOCKED`.

### 4. Manual Review
Admins use the `/dashboard/moderation` API to:
- View stats (pending, flagged, etc.).
- List flagged images.
- Submit a decision: `MANUALLY_APPROVED` or `MANUALLY_REJECTED`.

### 5. Cleanup
The `BlockedImageCleanupService` runs periodically to:
- Delete files from GCS for `BLOCKED` or `MANUALLY_REJECTED` images.
- Remove references from the respective entities (e.g., clear `user.photoUrl` or delete `ThriftStorePhoto`).
- Update `cleanup_at` timestamp.

## API Specification (Admin)

### Stats: `GET /dashboard/moderation/stats`
Returns counts of records in each moderation state.

### Review Queue: `GET /dashboard/moderation/flagged`
Returns a paginated list of items requiring manual review.

### Submit Decision: `PATCH /dashboard/moderation/{id}/review`
- **Input**: `{ "decision": "MANUALLY_APPROVED|MANUALLY_REJECTED", "notes": "string" }`
- **Effect**: Updates state and triggers immediate cleanup if rejected.

## Technical Details
- **Model Architecture**: Vision Transformer (likely) exported to ONNX.
- **Input Shape**: `[1, 3, 224, 224]` (NCHW).
- **Invariants**:
  - Images are processed in the background to avoid blocking user APIs.
  - Blocked content is hidden from users even before physical deletion (implicitly, by checking status if needed, though usually, cleanup clears the URL).

## Non-Goals
- Text-based moderation (not implemented).
- Video moderation (images only).
- Real-time rejection during upload (processing is async).

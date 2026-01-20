# Data Architecture and Entity Relationships

## Purpose
This document provides a high-level overview of the Meer platform's domain model, entity relationships, and the cascading logic that ensures data integrity.

## Entity Relationship Graph (Simplified)

### Core User and Store
- **AuthUser** (1) ↔ (0..1) **ThriftStore**: A user can own at most one store.
- **AuthUser** (N) ↔ (M) **ThriftStore** (via `auth_user_favorites`): Users can favorite multiple stores.
- **ThriftStore** (1) ↔ (N) **ThriftStorePhoto**: A store has multiple photos (ordered).

### Content and Engagement
- **ThriftStore** (1) ↔ (N) **GuideContent**: Stores originate articles and guides.
- **AuthUser** (1) ↔ (N) **GuideContentLike**: Users can like multiple contents.
- **AuthUser** (1) ↔ (N) **GuideContentComment**: Users can comment on content.
- **GuideContent** (1) ↔ (N) **GuideContentLike**.
- **GuideContent** (1) ↔ (N) **GuideContentComment**.

### Feedback and Support
- **AuthUser** (1) ↔ (N) **StoreFeedback** (Unique per User-Store): One rating/review per user per store.
- **SupportContact**: Independent entity for anonymous or authenticated support messages.

## Cascading Deletion and Integrity Logic
The system implements complex cascading logic via the `StoreDeletionService` and `DeleteUserUseCase` to prevent orphaned records and dangling binary assets.

### Store Deletion Flow
When a `ThriftStore` is deleted:
1. **Ownership**: The reference in the `AuthUser` (owner) is nulled.
2. **Favorites**: All user favorite records for this store are hard-deleted.
3. **Feedback**: All ratings and reviews for this store are hard-deleted.
4. **Guide Content**:
   - All associated `GuideContent` is collected.
   - All **Likes** and **Comments** on that content are hard-deleted.
   - All content records are hard-deleted.
5. **Asset Cleanup**: All URLs (Cover, Gallery, Photos, Content Images) are collected and enqueued in the `asset_deletion_job` queue.
6. **Persistence**: The `ThriftStore` record is removed.

### User Deletion Flow
When an `AuthUser` is deleted:
1. **Store Link**: The user's link to any owned store is nulled (The store itself remains, but becomes orphaned/ownerless).
2. **Favorites**: All user's personal favorites are hard-deleted.
3. **Audit Trail**: The `deleted_by` reference in `GuideContent` and `edited_by` in `GuideContentComment` are set to `NULL` to preserve content history.
4. **Engagement**: User's comments and likes are handled (typically hard-deleted or nulled based on configuration).

## Data Invariants
- **Atomic Counters**: `likeCount` and `commentCount` in `GuideContent` must match the actual record count.
- **Unique Feedback**: A user cannot have multiple `StoreFeedback` records for the same `storeId`.
- **Primary Keys**: All new entities use UUID v7 for time-ordered uniqueness.

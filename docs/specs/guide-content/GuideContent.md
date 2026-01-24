# Guide Content (CMS) Specification

## Purpose
The Guide Content module provides a CMS-like experience for stores to share articles, guides, or promotional content with users. It includes social engagement features like likes and comments.

## Scope
- Content lifecycle (creation, updates, soft-deletion)
- Social engagement (likes, comments)
- Media management for content images
- Moderation of community contributions (comments)

## Definitions
- **GuideContent**: An article or post associated with a store or global moderation (Admin content).
- **Global Content**: Posts created by Admins not linked to a specific store, appearing as "Guia Brechó".
- **Engagement**: The aggregate of likes and comments on a piece of content.
- **Soft Delete**: Marking content as deleted without physically removing the record, preserving audit trails.

## Content Model
| Field | Type | Description |
|-------|------|-------------|
| id | Integer | Unique identifier (Serial) |
| title | String | Content title |
| description | String | Content body/summary (Max 2048 chars) |
| type | String | Content type (e.g., "article", "promo") |
| categoryLabel| String | Category identifier |
| imageUrl | String | Main content image URL |
| thriftStore | UUID | Reference to the originating store (NULL for global content) |
| likeCount | Long | Denormalized like counter |
| commentCount | Long | Denormalized comment counter |
| createdAt | Instant | Creation timestamp |
| deletedAt | Instant | Timestamp of soft-deletion |

## Content API

### Discovery: `GET /contents`
- **Query Parameters**:
  - `q`: Search term.
  - `storeId`: Filter by specific store (can be empty for global content).
  - `page`, `pageSize`: Pagination.
  - `sort`: `newest` or `oldest`.
- **Behavior**: Returns active (non-deleted) contents enriched with engagement stats (likes, comments, and if the current user liked it).

### Details: `GET /contents/{id}`
- **Behavior**: Returns the full content entity with engagement summary. Content with no store association displays "Guia Brechó" as the origin.

## Social Engagement

### Likes
- **Like**: `POST /contents/{id}/likes`
- **Unlike**: `DELETE /contents/{id}/likes`
- **Behavior**: Toggles the user's like status and updates the denormalized `like_count`.

### Comments
- **List**: `GET /contents/{id}/comments` (Paginated)
- **Create**: `POST /contents/{id}/comments`
  - **Rate Limiting**: Enforced to prevent spam.
- **Edit**: `PATCH /contents/{id}/comments/{commentId}`
  - Allowed for: Author, Admin.
- **Delete**: `DELETE /contents/{id}/comments/{commentId}`
  - Allowed for: Author, Store Owner (where content is posted), Admin.
  - **Implementation**: Hard delete.

## Management API

### Creation: `POST /contents`
- **Authentication**: Required. User must be a store owner or admin.
- **Rules**:
  - Store Owners: Must provide a `storeId` they own.
  - Admins: May provide a `storeId` or leave it null to create a Global Post ("Guia Brechó").
- **Behavior**: Creates a new content entry.

### Media: `POST /contents/{id}/image/upload`
- **Behavior**: Provides a GCS signed URL for content image upload.

### Deletion: `DELETE /contents/{id}`
- **Authentication**: Required. Must be owner or admin.
- **Behavior**: Soft-deletes the content and records the actor and reason.

## Invariants
- Content must be linked to a valid `ThriftStore` OR be created by an `ADMIN` (Global Content).
- Only active content is visible in public listings.
- Denormalized counts (`like_count`, `comment_count`) should be kept in sync with the actual records.

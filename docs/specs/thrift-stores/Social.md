# Store Social Engagement Specification

## Purpose
The Social Engagement module manages user interactions with thrift stores, allowing users to build a personal favorites list and provide feedback through ratings and reviews.

## Scope
- Favorites system (User-Store associations)
- Ratings and Feedback system (Score and reviews)
- Social proof data aggregation (Averages and counts)

## Favorites System

### Behavior
Users can mark stores as favorites. This is an idempotent relationship stored in the `auth_user_favorites` table.

### Endpoints
- **List Favorites**: `GET /favorites`
  - Returns the list of stores favorited by the current user.
  - Supports `lat`, `lng` for distance calculation.
- **Add Favorite**: `POST /favorites/{storeId}`
  - Adds the store to the user's favorites list.
- **Remove Favorite**: `DELETE /favorites/{storeId}`
  - Removes the store from the user's favorites list.

### Versioning: `GET /favorites/versioned`
To optimize mobile sync, the system provides a "versioned" view of favorites.
- **Output**: A list of all favorited `storeId`s (sorted) and a `version` hash (Hex string).
- **Use Case**: Allows clients to quickly check if their local favorite cache is up-to-date by comparing the hash.

## Ratings and Feedback

### Model
| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| score | Integer | Numeric rating | 1 to 5 |
| body | String | Review text | Optional |
| createdAt | Instant | Timestamp | Auto-generated |

### Upsert Workflow: `POST /stores/{storeId}/feedback`
- **Behavior**: Users can only have **one** feedback entry per store. Posting to this endpoint will create a new entry or update an existing one for the current user.
- **Validation**: Score must be between 1 and 5.

### Consumption API
- **My Feedback**: `GET /stores/{storeId}/feedback`
  - Returns the authenticated user's review for the store.
- **Public Ratings**: `GET /stores/{storeId}/ratings`
  - Paginated list of all ratings and reviews for a store.
- **Aggregates**:
  - Global `rating` (average) and `reviewCount` are included in most store DTOs (Discovery, Search, Details).

## Invariants
- A user cannot rate a store they haven't "interacted" with (implicitly enforced by being a public API, but logically restricted to one per user).
- Ratings are stored as `Integer` but averages are returned as `Double`.
- Deleting feedback is idempotent.

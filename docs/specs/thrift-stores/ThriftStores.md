# Thrift Stores Specification

## Purpose
The Thrift Stores module manages the registry of stores, their locations, contact information, media, and taxonomy.

## Scope
- Store registration and management
- Store discovery (search, nearby, filtering)
- Store media (photos and gallery)
- Store ownership and authorization
- Store-category associations

## Definitions
- **ThriftStore**: A physical or virtual establishment selling second-hand items.
- **Store Owner**: An `AuthUser` linked to a store, granted permissions to manage it.
- **Taxonomy**: A set of categories (e.g., "Vintage", "Furniture") assigned to stores.

## Store Model
| Field | Type | Description |
|-------|------|-------------|
| id | UUID (v7) | Unique identifier |
| name | String | Store name |
| tagline | String | Short promotional text |
| description | String | Detailed description (Max 1000 chars) |
| addressLine | String | Physical address |
| neighborhood | String | Area or district |
| latitude | Double | Geographic coordinate |
| longitude | Double | Geographic coordinate |
| phone | String | Contact phone |
| email | String | Contact email |
| website | String (Social)| Website URL |
| instagram | String (Social)| Instagram handle/URL |
| facebook | String (Social)| Facebook handle/URL |
| whatsapp | String (Social)| WhatsApp contact |
| coverImageUrl | String | Main banner image URL |
| categories | List<String> | Taxonomy labels |
| badgeLabel | String | Special status label (e.g., "Verified") |
| openingHours | String | Business hours text |

## Discovery API

### Listing and Search: `GET /stores`
- **Query Parameters**:
  - `q`: Search term (matches name, description, neighborhood).
  - `categoryId`: Filter by specific category.
  - `type`: Specific store type filter (implementation detail).
  - `lat`, `lng`: User coordinates for distance calculation and sorting.
  - `page`, `pageSize`: Pagination (1-based).
- **Behavior**: Returns a paginated list of stores. If coordinates are provided, includes distance.
- **Authentication**: Optional. If authenticated, includes `isFavorite` status.

### Store Details: `GET /stores/{id}`
- **Behavior**: Returns full store profile including:
  - Associated `GuideContent` summaries.
  - Global rating and review count.
  - Authenticated user's own rating (if applicable).
  - Favorite status.

### Global Search: `GET /stores/search`
- **Inputs**: `q` (Query string), `page`, `pageSize`.
- **Behavior**: 
  - Performs a text-based search across store names, descriptions, and neighborhoods.
  - Returns a paginated list of `ThriftStoreDto`.
  - Enriches results with ratings and favorite status.
- **Constraints**: `q` is mandatory. Page size is capped at 50.

## Store Management

### Creation: `POST /stores`
- **Authentication**: Required.
- **Behavior**: Creates a new store record. The requesting user becomes the owner.

### Updates: `PUT /stores/{id}`
- **Authentication**: Required. Must be owner or admin.
- **Behavior**: Updates store fields.

### Deletion: `DELETE /stores/{id}`
- **Authentication**: Required. Must be owner or admin.
- **Behavior**: Triggers `DeleteThriftStoreUseCase`, which cleans up photos, contents, and associations.

## Media Management
- **Photo Uploads**: `POST /stores/{storeId}/photos/uploads`
  - Requests GCS signed URLs for multiple images.
- **Photo Update**: `PUT /stores/{storeId}/photos`
  - Replaces the store's photo collection with new/existing keys.
  - **Moderation**: New photos are enqueued for NSFW detection.

## Invariants
- A store must have at least a name and an address.
- Geographic coordinates are preferred for nearby search functionality.
- Owners are limited to managing their own stores unless they have the ADMIN role.
- Photo URLs must be valid and (usually) hosted on the project's GCS bucket.

## Feedback and Ratings
- Managed via `StoreRatingsController` and `StoreFeedbackController` (see separate spec if needed).
- Ratings are numeric (1-5).
- Feedback includes optional text body.

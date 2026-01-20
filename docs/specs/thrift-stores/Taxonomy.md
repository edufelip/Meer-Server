# Taxonomy and Categories Specification

## Purpose
The Taxonomy module defines the classification system for the Meer platform. Categories are the primary organizational unit used to group stores and guide content, enabling structured discovery and filtering.

## Scope
- Category model and persistence.
- Store-category associations.
- Public consumption of categorized listings.
- Administrative management (CRUD) of categories.

## Category Model
| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| id | String | Machine-readable ID (e.g., "vintage") | Primary Key |
| nameStringId | String | Reference to a human-readable name | Required |
| imageResId | String | Reference to an icon or illustration | Required |
| createdAt | Instant | Creation timestamp | Auto-generated |

## Public API

### List All: `GET /categories`
- **Behavior**: Returns the full list of available categories.
- **Caching**: Results are cached for 60 minutes (`categoriesAll` cache).

### Categorized Stores: `GET /categories/{categoryId}/stores`
- **Inputs**: `categoryId` (path), `page`, `pageSize`.
- **Behavior**: 
  - Validates existence of the category.
  - Returns a paginated list of stores tagged with the specified category ID.
  - Returns `404 Not Found` if the category does not exist.

## Store and Content Associations
- **Stores**: A `ThriftStore` can have multiple category labels in its `categories` collection.
- **Guide Content**: Each `GuideContent` has a single `categoryLabel`.

## Administrative Management: `/dashboard/categories`
Restricted to users with the `ADMIN` role.

### CRUD Operations
1. **List**: `GET /dashboard/categories` (Paginated, ordered by ID).
2. **Create**: `POST /dashboard/categories`
   - Validates that the ID is unique.
3. **Update**: `PUT /dashboard/categories/{id}`
   - Allows updating the display name (string ID) and image resource reference.
4. **Delete**: `DELETE /dashboard/categories/{id}`
   - Removes the category definition.
   - **Warning**: Deleting a category does not automatically scrub labels from stores/content (Soft-referencing via strings).

## Invariants
- Category IDs are strings (not UUIDs) to allow for readable URL paths and easier seeding.
- Taxonomy is semi-static; changes via the dashboard are infrequent but available for system flexibility.

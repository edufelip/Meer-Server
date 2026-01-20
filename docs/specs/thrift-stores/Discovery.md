# Discovery and Exploration Specification

## Purpose
The Discovery module provides curated and location-aware entry points for users to explore stores and content. It optimizes for initial user engagement by combining multiple domains (Stores, CMS, Ratings) into unified responses.

## Scope
- Home feed (Unified discovery)
- Featured content and stores
- Location-based (Nearby) discovery
- Discovery-specific caching

## Unified Home Feed: `GET /home`
The home feed is the primary entry point for mobile and web clients.

### Inputs
- `lat`, `lng`: Required. User's geographic coordinates.
- `Authorization`: Optional. Used to personalize favorite and like statuses.

### Composition
The response is a composite of:
1. **Featured Stores**: Top 10 recently updated or curated stores (Cached).
2. **Nearby Stores**: Top 10 stores geographically closest to the user's coordinates.
3. **Featured Guides**: Top 10 recently published guide articles (Cached).

### Behavior
- For each store returned (Featured or Nearby), the response includes:
  - Global rating and review count.
  - User-specific favorite status (if authenticated).
- For each guide returned, the response includes:
  - Engagement counts (likes/comments).
  - User-specific "liked" status (if authenticated).

## Curated Content: `GET /featured`
- **Behavior**: Returns the `featuredTop10` stores.
- **Caching**: This endpoint is heavily cached (`featuredTop10` cache manager) for 10 minutes.

## Location-Based Discovery: `GET /nearby`
- **Inputs**: `lat`, `lng` (required), `pageIndex`, `pageSize`.
- **Behavior**: Uses PostGIS spatial queries to find stores ordered by distance from the provided coordinates.
- **Output**: `NearbyStoreDto` which explicitly includes the calculated distance in meters.

## Performance and Invariants
- **Caching**: Discovery endpoints rely on the `featuredTop10` and `guideTop10` caches to maintain low latency.
- **Fallback**: If coordinates are invalid, the system should return an error or a default non-location-aware list (Current implementation requires coordinates for `/home` and `/nearby`).
- **Data Freshness**: Cache TTL of 5-10 minutes ensures that new stores or articles appear quickly on the home screen.

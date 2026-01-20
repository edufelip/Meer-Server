# Administration and Support Specification

## Purpose
The Administration module provides a centralized dashboard for managing users, stores, content, categories, and support requests. It is restricted to users with the `ADMIN` role.

## Scope
- Global dashboard analytics (counters and summaries)
- Category taxonomy management
- Support request handling (Contact us)
- User and Store management (Audit and deletion)

## Administrative Security
- **Restricted Access**: All routes under `/dashboard/**` require an `ADMIN` role.
- **Guard Mechanism**: `DashboardAdminGuardFilter` and `AdminContext` ensure that only authorized administrators can interact with dashboard endpoints.

## Support System

### Public Contact: `POST /support/contact`
- **Inputs**: `name`, `email`, `message`.
- **Validation**:
  - Simple email regex validation.
  - All fields are required.
- **Rate Limiting**: Enforced per IP address to prevent denial of service via support requests.
- **Persistence**: Stored in `support_contact` table for administrative review.

### Administrative Management: `/dashboard/support`
- **List Contacts**: `GET /dashboard/support/contacts` (Paginated, newest first).
- **Get Detail**: `GET /dashboard/support/contacts/{id}`.
- **Delete**: `DELETE /dashboard/support/contacts/{id}`.

## Category Management
- **Public List**: `GET /categories` (Cached for 60m).
- **Admin Management**: `/dashboard/categories` (CRUD operations for the taxonomy).
  - Categories consist of an ID, a human-readable name, and an image reference.

## Global Dashboard Capabilities

### Metrics and Analytics
- The dashboard provides statistics on:
  - User growth.
  - Active stores.
  - Guide content engagement.
  - Image moderation queue status.

### Content Oversight
- **Guide Content**: Admins can soft-delete any content and restore previously deleted content.
- **Stores**: Admins can delete any store registry.
- **Users**: Admins can delete user accounts (e.g., for policy violations).

## Invariants
- Admin actions are often "hard" operations (e.g., physical deletion of support records) or recorded with an audit trail (e.g., soft-deletion reason).
- Rate limits on support contacts are stricter than general API limits to prevent mail/DB spam.

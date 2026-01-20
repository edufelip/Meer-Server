# Notifications Specification

## Purpose
The Notifications module handles cross-platform push notifications using Firebase Cloud Messaging (FCM). It manages device tokens and provides targeted or broadcast messaging capabilities.

## Scope
- Device token registration and management.
- Integration with Firebase Cloud Messaging (FCM).
- Target audience selection (User, Device, Topic).
- Error handling and token lifecycle management.

## Definitions
- **FCM Token**: A unique identifier provided by Firebase for a specific app instance on a device.
- **DeviceId**: A stable identifier for a device installation.
- **Environment**: Deployment stage (DEV, STAGING, PROD) to isolate notification traffic.
- **Topic**: A logical channel for broadcasts (e.g., `new_stores-dev`).

## Token Management

### Registration: `POST /push-tokens`
- **Inputs**: `deviceId`, `fcmToken`, `platform` (ANDROID/IOS), `appVersion`, `environment`.
- **Behavior**: Upserts a `PushToken` record for the authenticated user.
- **Invariants**: A user can have multiple tokens (multiple devices/environments).

### Deletion: `DELETE /push-tokens/{deviceId}`
- **Behavior**: Removes a specific device token for the user. Used primarily during logout.

## Administrative Push API
Admins can send push notifications manually via the `/dashboard/push` suite. All endpoints require `ADMIN` role.

### 1. Send Test Push: `POST /dashboard/push`
- **Inputs**: `token` (FCM token or stored PushToken ID), `title`, `body`, `type`, `id`.
- **Behavior**: Sends a one-off notification to a specific device.

### 2. User Broadcast: `POST /dashboard/push/user`
- **Inputs**: `userId`, `environment`, `title`, `body`, `type`, `id`.
- **Behavior**: Sends a notification to all devices registered to a specific user in the target environment.

### 3. Audience Broadcast (Topics): `POST /dashboard/push/broadcast`
- **Inputs**: `audience` (promos/new_stores), `environment`, `title`, `body`, `type`, `id`.
- **Behavior**: Sends a notification to a predefined FCM topic. Topic name is resolved as `{audience}-{environment}` (e.g., `promos-dev`).

### Payload Routing
The mobile app routes based on the `type` data field:
- `guide_content`: Opens the guide detail screen.
- `store`: Opens the thrift store detail screen.

## Notification Delivery

### Target Types
1. **To User**: `sendToUser(userId, environment, ...)`
   - Sends to all registered tokens for that user in the specified environment.
2. **To Topic**: `sendToTopic(topic, ...)`
   - Broadcasts to all devices subscribed to the topic. Topic naming convention: `{base}-{environment}` (e.g., `promos-prod`).
3. **To Single Token**: `sendTestPush(token, ...)`
   - Targeted delivery to a specific FCM token or a stored `PushToken` ID (UUID).

### Payload Structure
- **Notification**: Standard `title` and `body` for system-level display.
- **Data**: Key-value pairs for in-app routing (e.g., `type: "store", id: "uuid"`).
- **Android Settings**: Uses channel ID `default`.
- **iOS Settings**: Uses default sound.

## Reliability and Lifecycle

### Automatic Invalidation
If FCM returns `UNREGISTERED` or `NOT_FOUND` during a send attempt, the system automatically deletes the invalid token from the database.

### HTTP Fallback
If the Firebase Admin SDK fails due to authentication or transient errors, the system implements a manual HTTP POST fallback to the FCM v1 REST API to ensure delivery.

### Error Classification
Failures are categorized into reasons like:
- `TOKEN_INVALID`: Device no longer active.
- `QUOTA_EXCEEDED`: Rate limit reached.
- `PERMISSION_DENIED`: Server-side credential issue.

## Non-Goals
- In-app notification inbox (not implemented, push only).
- SMS or Email notifications (handled by other modules).

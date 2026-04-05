# Users API

Base path: `/api/users`
Authentication: All endpoints require a valid JWT token via `AUTH_TOKEN` HttpOnly cookie. No public endpoints.

---

## Endpoints

### GET /api/users/me

Retrieve the authenticated user's own profile. Returns full profile details including private fields (email, timezone).

**Authentication:** Required (JWT cookie)
**Status:** `200 OK`

**Response Body:**

```json
{
  "id": 1,
  "username": "testuser",
  "role": "USER",
  "email": "test@example.com",
  "avatarUrl": "https://avatar.url",
  "timezone": "UTC",
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

Note: `accountLockedUntil`, `failedLoginAttempts`, `deletedAt`, and `bannedAt` are omitted for regular users viewing their own profile (fields are null, and `@JsonInclude(NON_NULL)` suppresses them).

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `401 Unauthorized` | Missing/invalid/revoked token, or user is banned or soft-deleted | (no body from Spring Security entry point) |

**curl:**

```bash
curl -s http://localhost:8080/api/users/me \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

### GET /api/users/{userId}

Retrieve a user's profile by numeric ID. The response content varies based on the relationship between the requester and the target user.

**Authentication:** Required (JWT cookie)
**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | `String` (numeric ID) | Yes | The target user's numeric ID |

**Status:** `200 OK`

**Response when viewing own profile** (userId matches authenticated user):

```json
{
  "id": 1,
  "username": "testuser",
  "role": "USER",
  "email": "test@example.com",
  "avatarUrl": "https://avatar.url",
  "timezone": "UTC",
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Response when viewing another user's profile** (regular USER role):

Only public fields are returned. Private fields (`email`, `timezone`, `lastModifiedAt`) are excluded.

```json
{
  "id": 2,
  "username": "otheruser",
  "role": "USER",
  "avatarUrl": "https://other.avatar.url",
  "createdAt": "2026-03-13T10:30:00"
}
```

**Response when privileged user (MODERATOR+) views another user's profile:**

Full profile plus admin fields are returned. Null admin fields are omitted.

```json
{
  "id": 3,
  "username": "targetuser",
  "role": "USER",
  "email": "target@example.com",
  "avatarUrl": "https://target.avatar.url",
  "timezone": "America/Chicago",
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:30:00",
  "failedLoginAttempts": 0
}
```

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `404 Not Found` | User ID does not exist or is non-numeric | `{"status": 404, "error": "User Not Found", "message": "User not found", "path": "/api/users/999999", "timestamp": "..."}` |

**curl:**

```bash
curl -s http://localhost:8080/api/users/42 \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

### GET /api/users/{userId}/campaigns

Retrieve a paginated list of campaigns where the specified user is involved (as creator, GM, or player). Accessible by the target user themselves or users with MODERATOR+ role.

**Authentication:** Required (JWT cookie)
**Access:** Target user themselves OR MODERATOR/ADMIN/OWNER

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | `Long` | Yes | The target user's numeric ID |

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | `int` | `0` | Page number (zero-based) |
| `size` | `int` | `20` | Page size (max 100) |
| `expand` | `String` | — | Comma-separated fields to expand (e.g., `creator`, `gameMasters`, `players`) |

**Status:** `200 OK`

**Response Body:**

```json
{
  "content": [
    {
      "id": 1,
      "name": "My Campaign",
      "description": "A Daggerheart adventure",
      "creatorId": 42,
      "isEnded": false,
      "gameMasterIds": [42],
      "playerIds": [43, 44],
      "createdAt": "2026-03-13T10:30:00",
      "lastModifiedAt": "2026-03-13T10:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `403 Forbidden` | Regular user viewing another user's campaigns | `{"status": 403, "error": "Insufficient Permissions", "message": "You do not have permission to view this user's campaigns", ...}` |
| `404 Not Found` | User ID does not exist (MODERATOR+ only) | `{"status": 404, "error": "Not Found", "message": "User not found with id: 999", ...}` |

**Notes:**
- Regular users requesting another user's campaigns receive 403 (not 404), even if the user ID does not exist. This prevents user enumeration.
- MODERATOR+ users receive 404 for non-existent user IDs.
- See also: `GET /api/dh/campaigns/mine` for the authenticated user's own campaigns shortcut.

**curl:**

```bash
curl -s "http://localhost:8080/api/users/42/campaigns?page=0&size=20&expand=creator" \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

### PATCH /api/users/me

Update the authenticated user's profile. Supports partial updates — only provided (non-null) fields are modified.

**Authentication:** Required (JWT cookie)
**Status:** `200 OK`
**Content-Type:** `application/json`

**Request Body:**

All fields are optional. Only include fields to change.

```json
{
  "email": "newemail@example.com",
  "avatarUrl": "https://new.avatar.url",
  "timezone": "America/New_York"
}
```

**Partial update example** (only update timezone):

```json
{
  "timezone": "America/Los_Angeles"
}
```

**Response Body:**

Returns the full updated user profile:

```json
{
  "id": 1,
  "username": "testuser",
  "role": "USER",
  "email": "newemail@example.com",
  "avatarUrl": "https://new.avatar.url",
  "timezone": "America/New_York",
  "createdAt": "2026-03-13T10:30:00",
  "lastModifiedAt": "2026-03-13T10:35:00"
}
```

**Error Responses:**

| Status | Condition | Example Body |
|--------|-----------|-------------|
| `400 Bad Request` | Validation failure (invalid email format, field too long) | `{"status": 400, "error": "Validation Failed", "fieldErrors": {"email": "Invalid email format"}, "path": "/api/users/me", "timestamp": "..."}` |
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `404 Not Found` | Authenticated user not found in database | `{"status": 404, "error": "User Not Found", "message": "User not found", ...}` |
| `409 Conflict` | Email already registered by another user | `{"status": 409, "error": "User Already Exists", "message": "Email already registered", "path": "/api/users/me", "timestamp": "..."}` |

**Notes:**
- Setting `email` to the same value the user already has is allowed (no uniqueness check triggered).
- Username cannot be changed via this endpoint.

**curl:**

```bash
curl -s -X PATCH http://localhost:8080/api/users/me \
  --cookie "AUTH_TOKEN=<jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"email": "newemail@example.com", "avatarUrl": "https://new.avatar.url", "timezone": "America/New_York"}'
```

---

### POST /api/users/me/change-password

Change the authenticated user's password. Invalidates all existing JWT tokens for the user and clears the `AUTH_TOKEN` cookie. The user must re-authenticate after this call.

**Authentication:** Required (JWT cookie)
**Status:** `204 No Content`
**Content-Type:** `application/json`

**Request Body:**

```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword456!"
}
```

**Response:** No body. The `AUTH_TOKEN` cookie is set with `Max-Age=0` to clear it.

**Password Requirements:** Minimum 8 characters, maximum 100 characters. Must contain uppercase, lowercase, digit, and special character (validated server-side by `PasswordValidator`).

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `400 Bad Request` | Current password is incorrect | `{"status": 400, "error": "Invalid Password", "message": "Current password is incorrect", "path": "/api/users/me/change-password", "timestamp": "..."}` |
| `400 Bad Request` | New password too weak or validation field errors | Varies (validation error or password policy error) |
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `404 Not Found` | User not found | `{"status": 404, "error": "User Not Found", "message": "User not found", ...}` |

**curl:**

```bash
curl -s -X POST http://localhost:8080/api/users/me/change-password \
  --cookie "AUTH_TOKEN=<jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword": "Password123!", "newPassword": "NewPassword456!"}'
```

---

### DELETE /api/users/me

Soft-delete the authenticated user's account. Sets `deletedAt` timestamp on the user record, revokes all active tokens, and clears the `AUTH_TOKEN` cookie. The user cannot authenticate after this operation.

**Authentication:** Required (JWT cookie)
**Status:** `204 No Content`

**Response:** No body. The `AUTH_TOKEN` cookie is set with `Max-Age=0` to clear it.

**Error Responses:**

| Status | Condition | Body |
|--------|-----------|------|
| `401 Unauthorized` | Missing/invalid token | (no body) |
| `404 Not Found` | User not found | `{"status": 404, "error": "User Not Found", "message": "User not found", ...}` |

**curl:**

```bash
curl -s -X DELETE http://localhost:8080/api/users/me \
  --cookie "AUTH_TOKEN=<jwt_token>"
```

---

## Models

### UpdateUserRequest

Request DTO for `PATCH /api/users/me`. All fields are optional for partial updates.

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | `String` | No | `@Email`, max 255 chars | New email address |
| `avatarUrl` | `String` | No | Max 500 chars | URL to avatar image |
| `timezone` | `String` | No | Max 50 chars | IANA timezone identifier |

### ChangePasswordRequest

Request DTO for `POST /api/users/me/change-password`.

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `currentPassword` | `String` | Yes | `@NotBlank` | The user's current password |
| `newPassword` | `String` | Yes | `@NotBlank`, 8-100 chars | The new password (must pass server-side strength validation: uppercase, lowercase, digit, special character) |

### UserResponse

Response DTO for user profile endpoints. Uses `@JsonInclude(NON_NULL)` so null fields are omitted from the JSON output.

| Field | Type | Nullable | Visibility | Description |
|-------|------|----------|------------|-------------|
| `id` | `Long` | No | All | Auto-generated user ID |
| `username` | `String` | No | All | Unique username |
| `role` | `Role` | No | All | User's role (e.g., `USER`, `MODERATOR`, `ADMIN`, `OWNER`) |
| `email` | `String` | Yes | Self + Privileged | User's email address |
| `avatarUrl` | `String` | Yes | All | URL to avatar image |
| `timezone` | `String` | Yes | Self + Privileged | IANA timezone identifier |
| `createdAt` | `LocalDateTime` | No | All | Account creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Self + Privileged | Last profile update timestamp |
| `accountLockedUntil` | `LocalDateTime` | Yes | Privileged only | When the account lock expires |
| `failedLoginAttempts` | `Integer` | Yes | Privileged only | Count of failed login attempts (default 0) |
| `deletedAt` | `LocalDateTime` | Yes | Privileged only | Soft-deletion timestamp |
| `bannedAt` | `LocalDateTime` | Yes | Privileged only | Ban timestamp |

**Visibility levels:**
- **All** — returned to any authenticated requester
- **Self + Privileged** — returned only when viewing own profile or when requester has MODERATOR/ADMIN/OWNER role
- **Privileged only** — returned only when requester has MODERATOR/ADMIN/OWNER role viewing another user

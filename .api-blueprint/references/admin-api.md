# Admin API

Base path: `/api/admin`

Authentication: JWT token via `AUTH_TOKEN` HttpOnly cookie. All endpoints require authentication; individual endpoints enforce role restrictions via `@PreAuthorize`.

---

## Endpoints

### GET `/api/admin/login-history`

Retrieve all login attempts for security monitoring.

**Required Role:** OWNER or ADMIN

**Query Parameters:**

| Parameter | Type    | Required | Description                                    |
|-----------|---------|----------|------------------------------------------------|
| `limit`   | Integer | No       | Maximum number of results to return. If omitted, returns all. |

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "userId": 4,
    "usernameAttempted": "owner",
    "success": true,
    "failureReason": null,
    "ipAddress": "127.0.0.1",
    "userAgent": "Test Agent",
    "createdAt": "2026-03-13T10:30:00"
  }
]
```

**Errors:**

| Status | Condition          | Error Body                                                         |
|--------|--------------------|--------------------------------------------------------------------|
| 401    | No AUTH_TOKEN cookie | Unauthorized                                                      |
| 403    | Role is MODERATOR or USER | `{"status":403,"error":"Access Denied","message":"You do not have permission to access this resource","path":"/api/admin/login-history","timestamp":"..."}` |

**curl:**

```bash
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/login-history?limit=2"
```

---

### GET `/api/admin/login-history/user/{userId}`

Retrieve login attempts for a specific user.

**Required Role:** OWNER or ADMIN

**Path Parameters:**

| Parameter | Type | Required | Description         |
|-----------|------|----------|---------------------|
| `userId`  | Long | Yes      | The target user's ID |

**Query Parameters:**

| Parameter | Type    | Required | Description                                    |
|-----------|---------|----------|------------------------------------------------|
| `limit`   | Integer | No       | Maximum number of results to return. If omitted, returns all. |

**Response:** `200 OK`

```json
[
  {
    "id": 5,
    "userId": 7,
    "usernameAttempted": "user",
    "success": true,
    "failureReason": null,
    "ipAddress": "127.0.0.1",
    "userAgent": "Test Agent",
    "createdAt": "2026-03-13T10:30:00"
  },
  {
    "id": 6,
    "userId": 7,
    "usernameAttempted": "user",
    "success": false,
    "failureReason": null,
    "ipAddress": "127.0.0.1",
    "userAgent": "Test Agent",
    "createdAt": "2026-03-13T10:30:01"
  }
]
```

**Errors:**

| Status | Condition                  |
|--------|----------------------------|
| 401    | No AUTH_TOKEN cookie        |
| 403    | Role is MODERATOR or USER   |

**curl:**

```bash
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/login-history/user/7?limit=10"
```

---

### GET `/api/admin/login-history/ip/{ipAddress}`

Retrieve login attempts from a specific IP address.

**Required Role:** OWNER or ADMIN

**Path Parameters:**

| Parameter   | Type   | Required | Description                          |
|-------------|--------|----------|--------------------------------------|
| `ipAddress` | String | Yes      | The IP address to filter by (IPv4 or IPv6, max 45 chars) |

**Query Parameters:**

| Parameter | Type    | Required | Description                                    |
|-----------|---------|----------|------------------------------------------------|
| `limit`   | Integer | No       | Maximum number of results to return. If omitted, returns all. |

**Response:** `200 OK`

```json
[
  {
    "id": 10,
    "userId": 7,
    "usernameAttempted": "user",
    "success": true,
    "failureReason": null,
    "ipAddress": "192.168.1.100",
    "userAgent": "Test Agent",
    "createdAt": "2026-03-13T10:30:00"
  },
  {
    "id": 11,
    "userId": 3,
    "usernameAttempted": "admin",
    "success": true,
    "failureReason": null,
    "ipAddress": "192.168.1.100",
    "userAgent": "Test Agent",
    "createdAt": "2026-03-13T10:30:01"
  }
]
```

**Errors:**

| Status | Condition                  |
|--------|----------------------------|
| 401    | No AUTH_TOKEN cookie        |
| 403    | Role is MODERATOR or USER   |

**curl:**

```bash
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/login-history/ip/192.168.1.100?limit=2"
```

---

### POST `/api/admin/users/{userId}/ban`

Ban a user. Sets `bannedAt` timestamp and invalidates all of the target user's active JWT tokens.

**Required Role:** OWNER, ADMIN, or MODERATOR (must have a strictly higher role than the target user)

**Path Parameters:**

| Parameter | Type | Required | Description            |
|-----------|------|----------|------------------------|
| `userId`  | Long | Yes      | The ID of the user to ban |

**Request Body:** None

**Response:** `200 OK`

```json
{
  "id": 5,
  "username": "moderator",
  "role": "MODERATOR",
  "email": "moderator@example.com",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

Note: `UserResponse` uses `@JsonInclude(NON_NULL)`, so null fields (e.g., `avatarUrl`, `timezone`, `bannedAt`) are omitted from the response.

**Errors:**

| Status | Condition                                        | Error                        |
|--------|--------------------------------------------------|------------------------------|
| 401    | No AUTH_TOKEN cookie                              | Unauthorized                 |
| 403    | Role is USER                                      | `{"error":"Access Denied","message":"You do not have permission to access this resource",...}` |
| 403    | Actor's role is not strictly higher than target's  | `{"error":"Insufficient Permissions","message":"..."}` |
| 404    | Target userId does not exist                      | `{"error":"User Not Found","message":"User not found",...}` |

**Side effects:**
- All active JWT tokens for the banned user are revoked.

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/users/5/ban"
```

---

### POST `/api/admin/users/{userId}/unban`

Unban a user. Clears the `bannedAt` timestamp.

**Required Role:** OWNER, ADMIN, or MODERATOR (must have a strictly higher role than the target user)

**Path Parameters:**

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `userId`  | Long | Yes      | The ID of the user to unban |

**Request Body:** None

**Response:** `200 OK`

```json
{
  "id": 3,
  "username": "admin",
  "role": "ADMIN",
  "email": "admin@example.com",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Errors:**

| Status | Condition                                        | Error                        |
|--------|--------------------------------------------------|------------------------------|
| 401    | No AUTH_TOKEN cookie                              | Unauthorized                 |
| 403    | Role is USER                                      | `{"error":"Access Denied",...}` |
| 403    | Actor's role is not strictly higher than target's  | `{"error":"Insufficient Permissions",...}` |
| 404    | Target userId does not exist                      | `{"error":"User Not Found","message":"User not found",...}` |

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/users/3/unban"
```

---

### POST `/api/admin/users/{userId}/change-role`

Change a user's role. Invalidates all of the target user's active JWT tokens to force re-authentication with the new role.

**Required Role:** OWNER only

**Path Parameters:**

| Parameter | Type | Required | Description                       |
|-----------|------|----------|-----------------------------------|
| `userId`  | Long | Yes      | The ID of the user whose role to change |

**Request Body:** `application/json`

```json
{
  "newRole": "ADMIN"
}
```

| Field     | Type   | Required | Validation           | Description                |
|-----------|--------|----------|----------------------|----------------------------|
| `newRole` | String | Yes      | `@NotNull`; must be a valid `Role` enum value | The new role to assign |

**Response:** `200 OK`

```json
{
  "id": 7,
  "username": "user",
  "role": "ADMIN",
  "email": "user@example.com",
  "createdAt": "2026-03-13T10:00:00",
  "lastModifiedAt": "2026-03-13T10:30:00"
}
```

**Errors:**

| Status | Condition                      | Error                        |
|--------|--------------------------------|------------------------------|
| 400    | `newRole` is null or missing    | `{"status":400,"error":"Validation Failed","fieldErrors":{"newRole":"Role is required"},...}` |
| 400    | `newRole` is not a valid Role   | Deserialization error         |
| 401    | No AUTH_TOKEN cookie            | Unauthorized                 |
| 403    | Role is ADMIN, MODERATOR, or USER | `{"error":"Access Denied",...}` |
| 404    | Target userId does not exist    | `{"error":"User Not Found","message":"User not found",...}` |

**Side effects:**
- All active JWT tokens for the target user are revoked.

**curl:**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"newRole":"ADMIN"}' \
  "http://localhost:8080/api/admin/users/7/change-role"
```

---

## Models

### LoginAttemptResponse

Returned by all `/login-history` endpoints.

| Field               | Type          | Nullable | Description                                      |
|---------------------|---------------|----------|--------------------------------------------------|
| `id`                | Long          | No       | Unique identifier of the login attempt            |
| `userId`            | Long          | Yes      | ID of the user (null if username not found in DB) |
| `usernameAttempted` | String        | No       | The username that was submitted (max 100 chars)   |
| `success`           | Boolean       | No       | Whether the login succeeded                       |
| `failureReason`     | String        | Yes      | Reason for failure (max 100 chars), null on success |
| `ipAddress`         | String        | Yes      | Client IP address (max 45 chars for IPv6)         |
| `userAgent`         | String        | Yes      | Client User-Agent header (max 500 chars)          |
| `createdAt`         | LocalDateTime | No       | Timestamp of the attempt                          |

### ChangeRoleRequest

Request body for the change-role endpoint.

| Field     | Type   | Required | Validation                          |
|-----------|--------|----------|-------------------------------------|
| `newRole` | String | Yes      | Must be a valid `Role` enum value. `@NotNull(message = "Role is required")` |

---

## Enums

### Role

Hierarchy (highest to lowest): `OWNER > ADMIN > MODERATOR > USER`

| Value       | Description                                           |
|-------------|-------------------------------------------------------|
| `OWNER`     | Full system access. Only role that can change user roles. |
| `ADMIN`     | Can view login history, ban/unban lower-role users.    |
| `MODERATOR` | Can ban/unban users with strictly lower roles only.    |
| `USER`      | Default role. No access to any admin endpoints.        |

Database constraint: `VARCHAR(20) NOT NULL DEFAULT 'USER'` on the `users.role` column.

---

## Authorization Summary

| Endpoint                                 | OWNER | ADMIN | MODERATOR | USER |
|------------------------------------------|-------|-------|-----------|------|
| `GET /api/admin/login-history`           | Yes   | Yes   | No (403)  | No (403) |
| `GET /api/admin/login-history/user/{id}` | Yes   | Yes   | No (403)  | No (403) |
| `GET /api/admin/login-history/ip/{ip}`   | Yes   | Yes   | No (403)  | No (403) |
| `POST /api/admin/users/{id}/ban`         | Yes   | Yes   | Yes*      | No (403) |
| `POST /api/admin/users/{id}/unban`       | Yes   | Yes   | Yes*      | No (403) |
| `POST /api/admin/users/{id}/change-role` | Yes   | No (403) | No (403) | No (403) |

\* Ban/unban requires the actor's role to be strictly higher than the target user's role. A MODERATOR cannot ban another MODERATOR or anyone with a higher role. Attempting to do so returns `403` with `{"error":"Insufficient Permissions"}`.

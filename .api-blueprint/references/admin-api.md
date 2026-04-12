# Admin API

Base path: `/api/admin`

Authentication: JWT token via `AUTH_TOKEN` HttpOnly cookie. All endpoints require authentication; individual endpoints enforce role restrictions via `@PreAuthorize`.

---

## Endpoints

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

### ChangeRoleRequest

Request body for the change-role endpoint.

| Field     | Type   | Required | Validation                          |
|-----------|--------|----------|-------------------------------------|
| `newRole` | String | Yes      | Must be a valid `Role` enum value. `@NotNull(message = "Role is required")` |

---

### POST `/api/admin/search/reindex`

Rebuild the full-text search index by clearing all entries and re-indexing every active entity from the source repositories. An expensive admin-only operation intended for recovery scenarios such as repairing a corrupted index or backfilling after a bulk SQL data fix that bypassed JPA events. Requires `OWNER` role.

**Query parameters**

| Parameter | Type                   | Required | Description                                                                 |
|-----------|------------------------|----------|-----------------------------------------------------------------------------|
| `type`    | `SearchableEntityType` | No       | If provided, only that entity type is rebuilt. If omitted, all types are rebuilt. |

**Response**: `200 OK` with a JSON map.

| Field     | Type    | Description                                                |
|-----------|---------|------------------------------------------------------------|
| `scope`   | String  | `"ALL"` or the name of the rebuilt `SearchableEntityType`. |
| `indexed` | Integer | Total number of entities re-indexed.                       |

**Notes**

- Soft-deleted entities (those with a non-null `deletedAt`) are skipped.
- The `BEASTFORM` type has no backing repository in the codebase and cannot be repopulated through this endpoint. Its index entries are cleared but not rebuilt; use the initial Flyway migration for beastform data.
- The operation runs synchronously in a single transaction. For large datasets, expect a long request duration.

**cURL example**

```bash
# Rebuild everything
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/search/reindex"

# Rebuild only weapons
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/search/reindex?type=WEAPON"
```

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
| `POST /api/admin/users/{id}/ban`         | Yes   | Yes   | Yes*      | No (403) |
| `POST /api/admin/users/{id}/unban`       | Yes   | Yes   | Yes*      | No (403) |
| `POST /api/admin/users/{id}/change-role` | Yes   | No (403) | No (403) | No (403) |
| `POST /api/admin/search/reindex`         | Yes   | No (403) | No (403) | No (403) |

\* Ban/unban requires the actor's role to be strictly higher than the target user's role. A MODERATOR cannot ban another MODERATOR or anyone with a higher role. Attempting to do so returns `403` with `{"error":"Insufficient Permissions"}`.
